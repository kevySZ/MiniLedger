package com.kevy.ledger.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.kevy.ledger.data.db.LedgerDatabaseHelper
import com.kevy.ledger.domain.model.Account
import com.kevy.ledger.domain.model.Book
import com.kevy.ledger.domain.model.BookSummary
import com.kevy.ledger.domain.model.Category
import com.kevy.ledger.domain.model.CategoryStat
import com.kevy.ledger.domain.model.CategoryType
import com.kevy.ledger.domain.model.DailyStat
import com.kevy.ledger.domain.model.EntryDirection
import com.kevy.ledger.domain.model.LedgerTransaction
import com.kevy.ledger.domain.model.StatsSnapshot
import com.kevy.ledger.domain.model.TransactionFilter
import com.kevy.ledger.domain.model.TransactionInput
import com.kevy.ledger.domain.model.TransactionType
import com.kevy.ledger.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.time.YearMonth

class LedgerRepository(
    private val context: Context,
    private val databaseHelper: LedgerDatabaseHelper
) {
    fun ensureSeedData() {
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            if (queryLong(db, "SELECT COUNT(*) FROM books", emptyArray()) == 0L) {
                val bookId = insertBook(db, "默认账本", DEFAULT_BOOK_COLOR, "日常生活账本", true)
                insertAccount(db, bookId, "现金", "现金", 0L, "", true)
                seedDefaultCategories(db, bookId)
                setMeta(db, KEY_SELECTED_BOOK_ID, bookId.toString())
            } else {
                getBooks(includeArchived = true).forEach { ensureDefaultCategories(db, it.id) }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getSelectedBookId(): Long {
        val db = databaseHelper.readableDatabase
        val raw = queryString(db, "SELECT value FROM meta WHERE key = ?", arrayOf(KEY_SELECTED_BOOK_ID))
        return raw?.toLongOrNull() ?: getBooks().firstOrNull()?.id ?: 0L
    }

    fun setSelectedBookId(bookId: Long) {
        setMeta(databaseHelper.writableDatabase, KEY_SELECTED_BOOK_ID, bookId.toString())
    }

    fun getCurrentBook(): Book? = getBook(getSelectedBookId())

    fun getBooks(includeArchived: Boolean = false): List<Book> {
        val db = databaseHelper.readableDatabase
        val selection = if (includeArchived) "" else " WHERE is_archived = 0"
        val cursor = db.rawQuery("SELECT * FROM books$selection ORDER BY is_default DESC, updated_at DESC", null)
        return cursor.useRows { rows ->
            rows.map {
                Book(
                    id = it.getLong("id"),
                    name = it.getString("name"),
                    colorHex = it.getString("color_hex"),
                    note = it.getString("note"),
                    isDefault = it.getInt("is_default") == 1,
                    isArchived = it.getInt("is_archived") == 1
                )
            }
        }
    }

    fun getBook(bookId: Long): Book? {
        val db = databaseHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM books WHERE id = ?", arrayOf(bookId.toString()))
        return cursor.useRows { rows ->
            rows.firstOrNull()?.let {
                Book(
                    id = it.getLong("id"),
                    name = it.getString("name"),
                    colorHex = it.getString("color_hex"),
                    note = it.getString("note"),
                    isDefault = it.getInt("is_default") == 1,
                    isArchived = it.getInt("is_archived") == 1
                )
            }
        }
    }

    fun saveBook(book: Book): Long {
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            val id = if (book.id == 0L) {
                val inserted = insertBook(db, book.name, book.colorHex, book.note, book.isDefault)
                insertAccount(db, inserted, "现金", "现金", 0L, "", true)
                seedDefaultCategories(db, inserted)
                inserted
            } else {
                if (book.isDefault) {
                    db.execSQL("UPDATE books SET is_default = 0")
                }
                val values = ContentValues().apply {
                    put("name", book.name)
                    put("color_hex", book.colorHex)
                    put("note", book.note)
                    put("is_default", if (book.isDefault) 1 else 0)
                    put("is_archived", if (book.isArchived) 1 else 0)
                    put("updated_at", nowString())
                }
                db.update("books", values, "id = ?", arrayOf(book.id.toString()))
                book.id
            }
            if (book.isDefault || getSelectedBookId() == 0L) {
                setMeta(db, KEY_SELECTED_BOOK_ID, id.toString())
            }
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    fun archiveBook(bookId: Long) {
        val books = getBooks()
        require(books.count { !it.isArchived } > 1) { "至少保留一个账本" }
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put("is_archived", 1)
            put("is_default", 0)
            put("updated_at", nowString())
        }
        db.update("books", values, "id = ?", arrayOf(bookId.toString()))
        if (getSelectedBookId() == bookId) {
            getBooks().firstOrNull { !it.isArchived && it.id != bookId }?.let { setSelectedBookId(it.id) }
        }
    }

    fun getAccounts(bookId: Long, includeInactive: Boolean = false): List<Account> {
        val db = databaseHelper.readableDatabase
        val selection = if (includeInactive) "" else " AND is_active = 1"
        val cursor = db.rawQuery(
            "SELECT * FROM accounts WHERE book_id = ?$selection ORDER BY is_active DESC, name ASC",
            arrayOf(bookId.toString())
        )
        val accounts = cursor.useRows { rows ->
            rows.map {
                Account(
                    id = it.getLong("id"),
                    bookId = it.getLong("book_id"),
                    name = it.getString("name"),
                    type = it.getString("type"),
                    initialBalanceCents = it.getLong("initial_balance_cents"),
                    note = it.getString("note"),
                    isActive = it.getInt("is_active") == 1
                )
            }
        }
        val balanceMap = calculateAccountBalanceMap(bookId)
        return accounts.map { it.copy(currentBalanceCents = balanceMap[it.id] ?: it.initialBalanceCents) }
    }

    fun getAccount(accountId: Long): Account? {
        val db = databaseHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM accounts WHERE id = ?", arrayOf(accountId.toString()))
        return cursor.useRows { rows ->
            rows.firstOrNull()?.let {
                val base = Account(
                    id = it.getLong("id"),
                    bookId = it.getLong("book_id"),
                    name = it.getString("name"),
                    type = it.getString("type"),
                    initialBalanceCents = it.getLong("initial_balance_cents"),
                    note = it.getString("note"),
                    isActive = it.getInt("is_active") == 1
                )
                base.copy(currentBalanceCents = calculateAccountBalanceMap(base.bookId)[base.id] ?: base.initialBalanceCents)
            }
        }
    }

    fun saveAccount(account: Account): Long {
        val db = databaseHelper.writableDatabase
        return if (account.id == 0L) {
            insertAccount(db, account.bookId, account.name, account.type, account.initialBalanceCents, account.note, account.isActive)
        } else {
            val values = ContentValues().apply {
                put("name", account.name)
                put("type", account.type)
                put("initial_balance_cents", account.initialBalanceCents)
                put("note", account.note)
                put("is_active", if (account.isActive) 1 else 0)
                put("updated_at", nowString())
            }
            db.update("accounts", values, "id = ?", arrayOf(account.id.toString()))
            account.id
        }
    }

    fun getCategories(bookId: Long, type: CategoryType? = null, includeInactive: Boolean = false): List<Category> {
        val db = databaseHelper.readableDatabase
        val args = mutableListOf(bookId.toString())
        val conditions = mutableListOf("book_id = ?")
        if (type != null) {
            conditions += "type = ?"
            args += type.name
        }
        if (!includeInactive) {
            conditions += "is_active = 1"
        }
        val cursor = db.rawQuery(
            "SELECT * FROM categories WHERE ${conditions.joinToString(" AND ")} ORDER BY type ASC, id ASC",
            args.toTypedArray()
        )
        return cursor.useRows { rows ->
            rows.map {
                Category(
                    id = it.getLong("id"),
                    bookId = it.getLong("book_id"),
                    name = it.getString("name"),
                    type = CategoryType.fromValue(it.getString("type")),
                    colorHex = it.getString("color_hex"),
                    isActive = it.getInt("is_active") == 1
                )
            }
        }
    }

    fun getCategory(categoryId: Long): Category? {
        val db = databaseHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM categories WHERE id = ?", arrayOf(categoryId.toString()))
        return cursor.useRows { rows ->
            rows.firstOrNull()?.let {
                Category(
                    id = it.getLong("id"),
                    bookId = it.getLong("book_id"),
                    name = it.getString("name"),
                    type = CategoryType.fromValue(it.getString("type")),
                    colorHex = it.getString("color_hex"),
                    isActive = it.getInt("is_active") == 1
                )
            }
        }
    }

    fun saveCategory(category: Category): Long {
        val db = databaseHelper.writableDatabase
        return if (category.id == 0L) {
            val values = ContentValues().apply {
                put("book_id", category.bookId)
                put("name", category.name)
                put("type", category.type.name)
                put("color_hex", category.colorHex)
                put("is_active", if (category.isActive) 1 else 0)
                put("created_at", nowString())
                put("updated_at", nowString())
            }
            db.insertOrThrow("categories", null, values)
        } else {
            val values = ContentValues().apply {
                put("name", category.name)
                put("type", category.type.name)
                put("color_hex", category.colorHex)
                put("is_active", if (category.isActive) 1 else 0)
                put("updated_at", nowString())
            }
            db.update("categories", values, "id = ?", arrayOf(category.id.toString()))
            category.id
        }
    }

    fun getTransaction(transactionId: Long): LedgerTransaction? {
        val db = databaseHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM transactions WHERE id = ?", arrayOf(transactionId.toString()))
        return cursor.useRows { rows -> rows.firstOrNull()?.toTransaction() }
    }

    fun saveTransaction(input: TransactionInput): Long {
        validateTransactionInput(input)
        val db = databaseHelper.writableDatabase
        val category = input.categoryId?.let { getCategory(it) }
        val account = input.accountId?.let { getAccount(it) }
        val transferAccount = input.transferAccountId?.let { getAccount(it) }
        val values = ContentValues().apply {
            put("book_id", input.bookId)
            put("type", input.type.name)
            put("direction", input.direction.name)
            put("category_id", input.categoryId)
            put("category_name", category?.name)
            put("account_id", input.accountId)
            put("account_name", account?.name)
            put("transfer_account_id", input.transferAccountId)
            put("transfer_account_name", transferAccount?.name)
            put("amount_cents", input.amountCents)
            put("note", input.note)
            put("event_date", DateUtils.formatStorageDate(input.eventDate))
            put("is_deleted", 0)
            put("updated_at", nowString())
        }
        return if (input.id == null) {
            values.put("created_at", nowString())
            db.insertOrThrow("transactions", null, values)
        } else {
            db.update("transactions", values, "id = ?", arrayOf(input.id.toString()))
            input.id
        }
    }

    fun deleteTransaction(transactionId: Long) {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put("is_deleted", 1)
            put("updated_at", nowString())
        }
        db.update("transactions", values, "id = ?", arrayOf(transactionId.toString()))
    }

    fun getTransactions(filter: TransactionFilter): List<LedgerTransaction> {
        val db = databaseHelper.readableDatabase
        val args = mutableListOf(filter.bookId.toString())
        val conditions = mutableListOf("book_id = ?", "is_deleted = 0")
        filter.startDate?.let {
            conditions += "event_date >= ?"
            args += DateUtils.formatStorageDate(it)
        }
        filter.endDate?.let {
            conditions += "event_date <= ?"
            args += DateUtils.formatStorageDate(it)
        }
        filter.type?.let {
            conditions += "type = ?"
            args += it.name
        }
        filter.categoryId?.let {
            conditions += "category_id = ?"
            args += it.toString()
        }
        filter.accountId?.let {
            conditions += "(account_id = ? OR transfer_account_id = ?)"
            args += it.toString()
            args += it.toString()
        }
        filter.keyword?.takeIf { it.isNotBlank() }?.let {
            conditions += "(note LIKE ? OR category_name LIKE ?)"
            args += "%$it%"
            args += "%$it%"
        }
        filter.minAmountCents?.let {
            conditions += "amount_cents >= ?"
            args += it.toString()
        }
        filter.maxAmountCents?.let {
            conditions += "amount_cents <= ?"
            args += it.toString()
        }
        val sql = "SELECT * FROM transactions WHERE ${conditions.joinToString(" AND ")} ORDER BY event_date DESC, id DESC"
        val cursor = db.rawQuery(sql, args.toTypedArray())
        return cursor.useRows { rows -> rows.map { it.toTransaction() } }
    }

    fun getRecentTransactions(bookId: Long, limit: Int = 8): List<LedgerTransaction> {
        return getTransactions(TransactionFilter(bookId = bookId)).take(limit)
    }

    fun getMonthSummary(bookId: Long, month: YearMonth): BookSummary {
        val records = getTransactions(
            TransactionFilter(
                bookId = bookId,
                startDate = month.atDay(1),
                endDate = month.atEndOfMonth()
            )
        )
        var income = 0L
        var expense = 0L
        records.forEach { transaction ->
            when (transaction.type) {
                TransactionType.INCOME -> income += transaction.amountCents
                TransactionType.EXPENSE -> expense += transaction.amountCents
                else -> Unit
            }
        }
        return BookSummary(incomeCents = income, expenseCents = expense)
    }

    fun getStats(bookId: Long, month: YearMonth): StatsSnapshot {
        val records = getTransactions(
            TransactionFilter(
                bookId = bookId,
                startDate = month.atDay(1),
                endDate = month.atEndOfMonth()
            )
        )
        val summary = getMonthSummary(bookId, month)
        val expenseTotal = summary.expenseCents.coerceAtLeast(1)
        val incomeTotal = summary.incomeCents.coerceAtLeast(1)
        val expenseGroups = records.filter { it.type == TransactionType.EXPENSE }.groupBy { it.categoryName ?: "未分类" }
        val incomeGroups = records.filter { it.type == TransactionType.INCOME }.groupBy { it.categoryName ?: "未分类" }
        val dailyMap = records.groupBy { it.eventDate.dayOfMonth }

        val expenseStats = expenseGroups.map { (name, items) ->
            val amount = items.sumOf { it.amountCents }
            CategoryStat(name = name, amountCents = amount, percentage = amount.toFloat() / expenseTotal.toFloat())
        }.sortedByDescending { it.amountCents }

        val incomeStats = incomeGroups.map { (name, items) ->
            val amount = items.sumOf { it.amountCents }
            CategoryStat(name = name, amountCents = amount, percentage = amount.toFloat() / incomeTotal.toFloat())
        }.sortedByDescending { it.amountCents }

        val dailyStats = dailyMap.map { (day, items) ->
            DailyStat(
                dayOfMonth = day,
                incomeCents = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents },
                expenseCents = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
            )
        }.sortedBy { it.dayOfMonth }

        return StatsSnapshot(
            summary = summary,
            expenseStats = expenseStats,
            incomeStats = incomeStats,
            dailyStats = dailyStats
        )
    }

    fun exportBackupJson(): String {
        val db = databaseHelper.readableDatabase
        val root = JSONObject()
        root.put("version", 1)
        root.put("exported_at", nowString())
        root.put("selected_book_id", getSelectedBookId())
        root.put("books", dumpTable(db, "books"))
        root.put("accounts", dumpTable(db, "accounts"))
        root.put("categories", dumpTable(db, "categories"))
        root.put("transactions", dumpTable(db, "transactions"))
        root.put("meta", dumpTable(db, "meta"))
        return root.toString(2)
    }

    fun restoreBackupJson(raw: String) {
        val root = JSONObject(raw)
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("transactions", null, null)
            db.delete("categories", null, null)
            db.delete("accounts", null, null)
            db.delete("books", null, null)
            db.delete("meta", null, null)

            restoreTable(db, "books", root.getJSONArray("books"))
            restoreTable(db, "accounts", root.getJSONArray("accounts"))
            restoreTable(db, "categories", root.getJSONArray("categories"))
            restoreTable(db, "transactions", root.getJSONArray("transactions"))
            restoreTable(db, "meta", root.getJSONArray("meta"))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun exportCsv(bookId: Long, month: YearMonth? = null): String {
        val filter = month?.let {
            TransactionFilter(bookId = bookId, startDate = it.atDay(1), endDate = it.atEndOfMonth())
        } ?: TransactionFilter(bookId = bookId)
        val lines = mutableListOf("日期,类型,类别,账户,转入账户,金额,备注")
        getTransactions(filter).forEach {
            lines += listOf(
                DateUtils.formatStorageDate(it.eventDate),
                it.type.name,
                it.categoryName.orEmpty(),
                it.accountName.orEmpty(),
                it.transferAccountName.orEmpty(),
                it.amountCents.toString(),
                it.note.replace(",", "，")
            ).joinToString(",")
        }
        return lines.joinToString("\n")
    }

    private fun dumpTable(db: SQLiteDatabase, table: String): JSONArray {
        val cursor = db.rawQuery("SELECT * FROM $table", null)
        return JSONArray().also { jsonArray ->
            cursor.use {
                while (it.moveToNext()) {
                    val item = JSONObject()
                    for (index in 0 until it.columnCount) {
                        val key = it.getColumnName(index)
                        when (it.getType(index)) {
                            Cursor.FIELD_TYPE_INTEGER -> item.put(key, it.getLong(index))
                            Cursor.FIELD_TYPE_FLOAT -> item.put(key, it.getDouble(index))
                            Cursor.FIELD_TYPE_STRING -> item.put(key, it.getString(index))
                            Cursor.FIELD_TYPE_NULL -> item.put(key, JSONObject.NULL)
                            else -> item.put(key, it.getString(index))
                        }
                    }
                    jsonArray.put(item)
                }
            }
        }
    }

    private fun restoreTable(db: SQLiteDatabase, table: String, rows: JSONArray) {
        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            val values = ContentValues()
            row.keys().forEach { key ->
                if (row.isNull(key)) {
                    values.putNull(key)
                } else {
                    when (val value = row.get(key)) {
                        is Int -> values.put(key, value)
                        is Long -> values.put(key, value)
                        is Double -> values.put(key, value)
                        is String -> values.put(key, value)
                        is Boolean -> values.put(key, if (value) 1 else 0)
                        else -> values.put(key, value.toString())
                    }
                }
            }
            db.insertOrThrow(table, null, values)
        }
    }

    private fun validateTransactionInput(input: TransactionInput) {
        require(input.amountCents > 0) { "金额必须大于 0" }
        when (input.type) {
            TransactionType.EXPENSE, TransactionType.INCOME -> {
                require(input.categoryId != null) { "请选择分类" }
                require(input.accountId != null) { "请选择账户" }
            }

            TransactionType.TRANSFER -> {
                require(input.accountId != null && input.transferAccountId != null) { "请选择转出和转入账户" }
                require(input.accountId != input.transferAccountId) { "转入转出账户不能相同" }
            }

            TransactionType.BALANCE_ADJUSTMENT -> {
                require(input.accountId != null) { "请选择账户" }
                require(input.direction != EntryDirection.NONE) { "请选择调整方向" }
            }
        }
    }

    private fun calculateAccountBalanceMap(bookId: Long): Map<Long, Long> {
        val baseBalances = getRawAccounts(bookId).associate { it.id to it.initialBalanceCents }.toMutableMap()
        getTransactions(TransactionFilter(bookId = bookId)).forEach { transaction ->
            when (transaction.type) {
                TransactionType.INCOME -> {
                    transaction.accountId?.let { baseBalances[it] = (baseBalances[it] ?: 0L) + transaction.amountCents }
                }

                TransactionType.EXPENSE -> {
                    transaction.accountId?.let { baseBalances[it] = (baseBalances[it] ?: 0L) - transaction.amountCents }
                }

                TransactionType.TRANSFER -> {
                    transaction.accountId?.let { baseBalances[it] = (baseBalances[it] ?: 0L) - transaction.amountCents }
                    transaction.transferAccountId?.let { baseBalances[it] = (baseBalances[it] ?: 0L) + transaction.amountCents }
                }

                TransactionType.BALANCE_ADJUSTMENT -> {
                    transaction.accountId?.let {
                        val delta = if (transaction.direction == EntryDirection.IN) transaction.amountCents else -transaction.amountCents
                        baseBalances[it] = (baseBalances[it] ?: 0L) + delta
                    }
                }
            }
        }
        return baseBalances
    }

    private fun getRawAccounts(bookId: Long): List<Account> {
        val db = databaseHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM accounts WHERE book_id = ?", arrayOf(bookId.toString()))
        return cursor.useRows { rows ->
            rows.map {
                Account(
                    id = it.getLong("id"),
                    bookId = it.getLong("book_id"),
                    name = it.getString("name"),
                    type = it.getString("type"),
                    initialBalanceCents = it.getLong("initial_balance_cents"),
                    note = it.getString("note"),
                    isActive = it.getInt("is_active") == 1
                )
            }
        }
    }

    private fun insertBook(db: SQLiteDatabase, name: String, colorHex: String, note: String, isDefault: Boolean): Long {
        if (isDefault) {
            db.execSQL("UPDATE books SET is_default = 0")
        }
        val values = ContentValues().apply {
            put("name", name)
            put("color_hex", colorHex)
            put("note", note)
            put("is_default", if (isDefault) 1 else 0)
            put("is_archived", 0)
            put("created_at", nowString())
            put("updated_at", nowString())
        }
        return db.insertOrThrow("books", null, values)
    }

    private fun insertAccount(
        db: SQLiteDatabase,
        bookId: Long,
        name: String,
        type: String,
        initialBalanceCents: Long,
        note: String,
        isActive: Boolean
    ): Long {
        val values = ContentValues().apply {
            put("book_id", bookId)
            put("name", name)
            put("type", type)
            put("initial_balance_cents", initialBalanceCents)
            put("note", note)
            put("is_active", if (isActive) 1 else 0)
            put("created_at", nowString())
            put("updated_at", nowString())
        }
        return db.insertOrThrow("accounts", null, values)
    }

    private fun seedDefaultCategories(db: SQLiteDatabase, bookId: Long) {
        DEFAULT_EXPENSE_CATEGORIES.forEach { insertCategory(db, bookId, it.name, CategoryType.EXPENSE, it.colorHex) }
        DEFAULT_INCOME_CATEGORIES.forEach { insertCategory(db, bookId, it.name, CategoryType.INCOME, it.colorHex) }
    }

    private fun ensureDefaultCategories(db: SQLiteDatabase, bookId: Long) {
        renameCategoryIfNeeded(db, bookId, CategoryType.EXPENSE, "人情", "礼物")
        renameCategoryIfNeeded(db, bookId, CategoryType.EXPENSE, "用餐", "餐饮")
        renameCategoryIfNeeded(db, bookId, CategoryType.INCOME, "退税", "退款")

        val existing = mutableSetOf<String>()
        val cursor = db.rawQuery("SELECT name, type FROM categories WHERE book_id = ?", arrayOf(bookId.toString()))
        cursor.use {
            while (it.moveToNext()) {
                existing += "${it.getString(0)}|${it.getString(1)}"
            }
        }

        DEFAULT_EXPENSE_CATEGORIES.forEach { spec ->
            val key = "${spec.name}|${CategoryType.EXPENSE.name}"
            if (key !in existing) {
                insertCategory(db, bookId, spec.name, CategoryType.EXPENSE, spec.colorHex)
            }
        }
        DEFAULT_INCOME_CATEGORIES.forEach { spec ->
            val key = "${spec.name}|${CategoryType.INCOME.name}"
            if (key !in existing) {
                insertCategory(db, bookId, spec.name, CategoryType.INCOME, spec.colorHex)
            }
        }
    }

    private fun renameCategoryIfNeeded(
        db: SQLiteDatabase,
        bookId: Long,
        type: CategoryType,
        oldName: String,
        newName: String
    ) {
        val targetExists = queryLong(
            db,
            "SELECT COUNT(*) FROM categories WHERE book_id = ? AND type = ? AND name = ?",
            arrayOf(bookId.toString(), type.name, newName)
        ) > 0L
        if (targetExists) return

        val values = ContentValues().apply {
            put("name", newName)
            put("updated_at", nowString())
        }
        db.update(
            "categories",
            values,
            "book_id = ? AND type = ? AND name = ?",
            arrayOf(bookId.toString(), type.name, oldName)
        )
    }

    private fun insertCategory(
        db: SQLiteDatabase,
        bookId: Long,
        name: String,
        type: CategoryType,
        colorHex: String
    ) {
        val values = ContentValues().apply {
            put("book_id", bookId)
            put("name", name)
            put("type", type.name)
            put("color_hex", colorHex)
            put("is_active", 1)
            put("created_at", nowString())
            put("updated_at", nowString())
        }
        db.insertOrThrow("categories", null, values)
    }

    private fun setMeta(db: SQLiteDatabase, key: String, value: String) {
        val values = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        db.insertWithOnConflict("meta", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun queryLong(db: SQLiteDatabase, sql: String, args: Array<String>): Long {
        val cursor = db.rawQuery(sql, args)
        return cursor.use { if (it.moveToFirst()) it.getLong(0) else 0L }
    }

    private fun queryString(db: SQLiteDatabase, sql: String, args: Array<String>): String? {
        val cursor = db.rawQuery(sql, args)
        return cursor.use { if (it.moveToFirst()) it.getString(0) else null }
    }

    private fun <T> Cursor.useRows(mapper: (List<Row>) -> T): T {
        val rows = mutableListOf<Row>()
        use {
            while (it.moveToNext()) {
                rows += Row(it)
            }
        }
        return mapper(rows)
    }

    private class Row(cursor: Cursor) {
        private val values = mutableMapOf<String, Any?>()

        init {
            for (index in 0 until cursor.columnCount) {
                val key = cursor.getColumnName(index)
                values[key] = when (cursor.getType(index)) {
                    Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                    Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
                    Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
                    Cursor.FIELD_TYPE_NULL -> null
                    else -> cursor.getString(index)
                }
            }
        }

        fun getLong(key: String): Long = (values[key] as? Long) ?: 0L
        fun getString(key: String): String = values[key]?.toString().orEmpty()
        fun getInt(key: String): Int = getLong(key).toInt()

        fun toTransaction(): LedgerTransaction {
            return LedgerTransaction(
                id = getLong("id"),
                bookId = getLong("book_id"),
                type = TransactionType.fromValue(getString("type")),
                direction = EntryDirection.fromValue(getString("direction")),
                categoryId = values["category_id"] as? Long,
                categoryName = values["category_name"]?.toString(),
                accountId = values["account_id"] as? Long,
                accountName = values["account_name"]?.toString(),
                transferAccountId = values["transfer_account_id"] as? Long,
                transferAccountName = values["transfer_account_name"]?.toString(),
                amountCents = getLong("amount_cents"),
                note = getString("note"),
                eventDate = DateUtils.parseDate(getString("event_date"))
            )
        }
    }

    private fun nowString(): String = java.time.OffsetDateTime.now().toString()

    companion object {
        private const val KEY_SELECTED_BOOK_ID = "selected_book_id"
        private const val DEFAULT_BOOK_COLOR = "#8FC9B4"

        private data class DefaultCategorySpec(
            val name: String,
            val colorHex: String
        )

        private val DEFAULT_EXPENSE_CATEGORIES = listOf(
            DefaultCategorySpec("餐饮", "#8FC9B4"),
            DefaultCategorySpec("交通", "#A8C8E8"),
            DefaultCategorySpec("购物", "#F1BA95"),
            DefaultCategorySpec("住房", "#E8D097"),
            DefaultCategorySpec("医疗", "#E5AAA4"),
            DefaultCategorySpec("娱乐", "#CDBBE8"),
            DefaultCategorySpec("通讯", "#A4B8E3"),
            DefaultCategorySpec("学习", "#A7D6C6"),
            DefaultCategorySpec("旅行", "#93D3D0"),
            DefaultCategorySpec("礼物", "#E6AFC0"),
            DefaultCategorySpec("宠物", "#F0CD9B"),
            DefaultCategorySpec("其他", "#C9BDB3")
        )

        private val DEFAULT_INCOME_CATEGORIES = listOf(
            DefaultCategorySpec("工资", "#7EBEA7"),
            DefaultCategorySpec("奖金", "#EFC17E"),
            DefaultCategorySpec("兼职", "#B2C8E8"),
            DefaultCategorySpec("理财", "#8FB5D8"),
            DefaultCategorySpec("退款", "#A9D5B3"),
            DefaultCategorySpec("其他", "#C9BDB3")
        )
    }
}
