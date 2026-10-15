package com.aistudio.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.delay

/**
 * Modern Syntax Colors tailored for Dark/Code Studio themes.
 * Based on high-contrast VS Code / Darcula IDE palettes.
 */
object SyntaxTheme {
    val Background = Color(0xFF1E1E24)
    val HeaderBg = Color(0xFF26262F)
    val Border = Color(0xFF383846)
    val GutterBg = Color(0xFF1A1A20)
    val GutterDivider = Color(0xFF2E2E3A)
    val LineNumber = Color(0xFF6E7681)
    val DefaultText = Color(0xFFD4D4D4)

    val Keyword = Color(0xFFC586C0)           // Purple: if, else, return, class, fun, public
    val Type = Color(0xFF4EC9B0)              // Teal: int, String, boolean, List, void
    val Function = Color(0xFFDCDCAA)          // Warm yellow: functionName(), method calls
    val String = Color(0xFFCE9178)            // Peach / Coral: "strings", 'chars'
    val Number = Color(0xFFB5CEA8)            // Mint green: 123, 4.56, 0xFF
    val Boolean = Color(0xFF569CD6)           // Light blue: true, false, null, None
    val Comment = Color(0xFF6A9955)           // Calm forest green: //, /* */, #
    val Annotation = Color(0xFF4FC1FF)        // Cyan/Blue: @Override, @Composable
    val Variable = Color(0xFF9CDCFE)          // Light sky blue: identifier, json key
    val Operator = Color(0xFFD4D4D4)          // Operators: =, +, -, &&, ||
    val Punctuation = Color(0xFF8B949E)       // Brackets, parens, commas, semicolons
    val Tag = Color(0xFF569CD6)               // XML / HTML tags
    val Attribute = Color(0xFF9CDCFE)         // XML / HTML attributes
}

/**
 * High-performance, multi-language Syntax Highlighter for Compose.
 * Supports Java, Kotlin, Python, JavaScript/TypeScript, SQL, JSON, HTML/XML,
 * C/C++, C#, Bash/Shell, Go, Rust, and generic code.
 */
object SyntaxHighlighter {

    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "break", "case", "catch", "class", "const", "continue",
        "default", "do", "else", "enum", "extends", "final", "finally", "for", "goto",
        "if", "implements", "import", "instanceof", "interface", "native", "new", "package",
        "private", "protected", "public", "return", "static", "strictfp", "super", "switch",
        "synchronized", "this", "throw", "throws", "transient", "try", "volatile", "while",
        "yield", "record", "sealed", "permits"
    )

    private val JAVA_TYPES = setOf(
        "boolean", "byte", "char", "double", "float", "int", "long", "short", "void",
        "String", "Integer", "Boolean", "Long", "Double", "Float", "Character", "Byte", "Short",
        "Object", "Class", "List", "ArrayList", "Map", "HashMap", "Set", "HashSet",
        "Collections", "Arrays", "System", "Math", "Exception", "Throwable", "StringBuilder",
        "Scanner", "PrintStream", "Thread", "Runnable", "Future", "Optional"
    )

    private val KOTLIN_KEYWORDS = setOf(
        "as", "break", "class", "continue", "do", "else", "for", "fun", "if", "in",
        "interface", "is", "object", "package", "return", "super", "this", "throw", "try",
        "typealias", "val", "var", "when", "while", "by", "catch", "constructor",
        "finally", "get", "import", "init", "set", "where", "actual", "abstract",
        "annotation", "companion", "const", "crossinline", "data", "enum", "expect",
        "external", "final", "infix", "inline", "inner", "internal", "lateinit", "noinline",
        "open", "operator", "out", "override", "private", "protected", "public", "reified",
        "sealed", "suspend", "tailrec", "vararg", "it"
    )

    private val KOTLIN_TYPES = setOf(
        "Boolean", "Byte", "Char", "Double", "Float", "Int", "Long", "Short", "String",
        "Any", "Unit", "Nothing", "List", "MutableList", "Map", "MutableMap", "Set",
        "MutableSet", "Array", "Sequence", "Flow", "StateFlow", "SharedFlow", "CoroutineScope"
    )

    private val PYTHON_KEYWORDS = setOf(
        "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
        "del", "elif", "else", "except", "finally", "for", "from", "global", "if",
        "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise",
        "return", "try", "while", "with", "yield", "match", "case"
    )

    private val PYTHON_TYPES = setOf(
        "int", "float", "str", "bool", "list", "dict", "set", "tuple", "bytes", "bytearray",
        "range", "complex", "object", "type", "self", "cls", "Any", "Optional", "Union",
        "Callable", "Iterable", "print", "len", "range", "enumerate", "zip", "sum", "min", "max"
    )

    private val JS_KEYWORDS = setOf(
        "break", "case", "catch", "class", "const", "continue", "debugger", "default",
        "delete", "do", "else", "export", "extends", "finally", "for", "function", "if",
        "import", "in", "instanceof", "new", "return", "super", "switch", "this", "throw",
        "try", "typeof", "var", "void", "while", "with", "yield", "let", "static", "enum",
        "await", "async", "implements", "interface", "package", "private", "protected",
        "public", "type", "declare", "namespace", "from", "as", "readonly", "require"
    )

    private val JS_TYPES = setOf(
        "string", "number", "boolean", "symbol", "bigint", "any", "unknown", "never", "void",
        "object", "Array", "Promise", "Record", "Map", "Set", "Function", "Date", "RegExp",
        "Error", "Console", "console", "Math", "JSON", "Window", "Document"
    )

    private val SQL_KEYWORDS = setOf(
        "select", "from", "where", "insert", "into", "update", "delete", "create", "table",
        "drop", "alter", "add", "column", "join", "inner", "left", "right", "full", "outer",
        "on", "group", "by", "order", "having", "limit", "offset", "and", "or", "not", "in",
        "is", "null", "like", "as", "distinct", "count", "sum", "avg", "max", "min",
        "primary", "key", "foreign", "references", "index", "unique", "default", "check",
        "union", "all", "view", "trigger", "procedure", "database", "between", "exists",
        "case", "when", "then", "else", "end", "asc", "desc"
    )

    private val BOOLEANS = setOf(
        "true", "false", "null", "none", "nil", "undefined", "nan", "infinity"
    )

    /**
     * Parses and highlights code, returning an AnnotatedString with colorized tokens.
     */
    fun highlight(code: String, rawLanguage: String): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")

        val lang = rawLanguage.lowercase().trim()

        return when {
            lang == "json" -> highlightJson(code)
            lang in setOf("html", "xml", "svg") -> highlightXml(code)
            lang in setOf("sql", "sqlite", "mysql", "postgresql", "postgres") -> highlightSql(code)
            else -> highlightGenericCode(code, lang)
        }
    }

    private fun highlightGenericCode(code: String, lang: String): AnnotatedString {
        val keywords = when {
            lang in setOf("java") -> JAVA_KEYWORDS
            lang in setOf("kotlin", "kt") -> KOTLIN_KEYWORDS
            lang in setOf("python", "py") -> PYTHON_KEYWORDS
            lang in setOf("javascript", "js", "typescript", "ts", "jsx", "tsx") -> JS_KEYWORDS
            else -> JAVA_KEYWORDS + KOTLIN_KEYWORDS + PYTHON_KEYWORDS + JS_KEYWORDS
        }

        val types = when {
            lang in setOf("java") -> JAVA_TYPES
            lang in setOf("kotlin", "kt") -> KOTLIN_TYPES
            lang in setOf("python", "py") -> PYTHON_TYPES
            lang in setOf("javascript", "js", "typescript", "ts", "jsx", "tsx") -> JS_TYPES
            else -> JAVA_TYPES + KOTLIN_TYPES + PYTHON_TYPES + JS_TYPES
        }

        val allowsHashComments = lang in setOf("python", "py", "bash", "sh", "zsh", "shell", "ruby", "rb", "yaml", "yml", "r")

        return buildAnnotatedString {
            var i = 0
            val len = code.length

            while (i < len) {
                val c = code[i]

                // 1. Single Line Comments (// or #)
                if (c == '/' && i + 1 < len && code[i + 1] == '/') {
                    val end = code.indexOf('\n', i)
                    val commentEnd = if (end == -1) len else end
                    withStyle(SpanStyle(color = SyntaxTheme.Comment, fontStyle = FontStyle.Italic)) {
                        append(code.substring(i, commentEnd))
                    }
                    i = commentEnd
                    continue
                }

                if (allowsHashComments && c == '#') {
                    val end = code.indexOf('\n', i)
                    val commentEnd = if (end == -1) len else end
                    withStyle(SpanStyle(color = SyntaxTheme.Comment, fontStyle = FontStyle.Italic)) {
                        append(code.substring(i, commentEnd))
                    }
                    i = commentEnd
                    continue
                }

                // 2. Multi-line Comments (/* ... */)
                if (c == '/' && i + 1 < len && code[i + 1] == '*') {
                    val end = code.indexOf("*/", i + 2)
                    val commentEnd = if (end == -1) len else end + 2
                    withStyle(SpanStyle(color = SyntaxTheme.Comment, fontStyle = FontStyle.Italic)) {
                        append(code.substring(i, commentEnd))
                    }
                    i = commentEnd
                    continue
                }

                // 3. Multiline Strings (""" ... """)
                if (c == '"' && i + 2 < len && code[i + 1] == '"' && code[i + 2] == '"') {
                    val end = code.indexOf("\"\"\"", i + 3)
                    val strEnd = if (end == -1) len else end + 3
                    withStyle(SpanStyle(color = SyntaxTheme.String)) {
                        append(code.substring(i, strEnd))
                    }
                    i = strEnd
                    continue
                }

                // 4. Standard Strings ("..." or '...' or `...`)
                if (c == '"' || c == '\'' || (c == '`' && lang in setOf("javascript", "js", "typescript", "ts", "jsx", "tsx", "bash", "sh", "go"))) {
                    val quote = c
                    var j = i + 1
                    var escaped = false
                    while (j < len) {
                        val curr = code[j]
                        if (escaped) {
                            escaped = false
                        } else if (curr == '\\') {
                            escaped = true
                        } else if (curr == quote) {
                            j++
                            break
                        } else if (curr == '\n' && quote != '`') {
                            // Single line quote terminates at newline
                            break
                        }
                        j++
                    }
                    withStyle(SpanStyle(color = SyntaxTheme.String)) {
                        append(code.substring(i, j))
                    }
                    i = j
                    continue
                }

                // 5. Annotations (@Override, @Composable, @Test)
                if (c == '@' && i + 1 < len && (code[i + 1].isLetter() || code[i + 1] == '_')) {
                    var j = i + 1
                    while (j < len && (code[j].isLetterOrDigit() || code[j] == '.' || code[j] == '_')) {
                        j++
                    }
                    withStyle(SpanStyle(color = SyntaxTheme.Annotation, fontWeight = FontWeight.SemiBold)) {
                        append(code.substring(i, j))
                    }
                    i = j
                    continue
                }

                // 6. Numbers (Hex or Dec)
                if (c.isDigit() || (c == '.' && i + 1 < len && code[i + 1].isDigit())) {
                    val start = i
                    var isHex = false
                    if (c == '0' && i + 1 < len && (code[i + 1] == 'x' || code[i + 1] == 'X')) {
                        isHex = true
                        i += 2
                    }
                    while (i < len) {
                        val ch = code[i]
                        if (isHex && (ch.isDigit() || ch in 'a'..'f' || ch in 'A'..'F')) {
                            i++
                        } else if (!isHex && (ch.isDigit() || ch == '.' || ch == 'f' || ch == 'F' || ch == 'L' || ch == 'l' || ch == 'e' || ch == 'E' || ch == 'd' || ch == 'D')) {
                            i++
                        } else {
                            break
                        }
                    }
                    withStyle(SpanStyle(color = SyntaxTheme.Number)) {
                        append(code.substring(start, i))
                    }
                    continue
                }

                // 7. Words (Identifiers, Keywords, Types, Booleans, Functions)
                if (c.isLetter() || c == '_' || c == '$') {
                    val start = i
                    while (i < len && (code[i].isLetterOrDigit() || code[i] == '_' || code[i] == '$')) {
                        i++
                    }
                    val word = code.substring(start, i)
                    val lowerWord = word.lowercase()

                    // Check if followed by open parenthesis -> function call
                    var isFunc = false
                    var peek = i
                    while (peek < len && code[peek].isWhitespace()) peek++
                    if (peek < len && code[peek] == '(' && lowerWord !in keywords) {
                        isFunc = true
                    }

                    when {
                        lowerWord in BOOLEANS -> {
                            withStyle(SpanStyle(color = SyntaxTheme.Boolean, fontWeight = FontWeight.Bold)) {
                                append(word)
                            }
                        }
                        word in keywords || lowerWord in keywords -> {
                            withStyle(SpanStyle(color = SyntaxTheme.Keyword, fontWeight = FontWeight.Bold)) {
                                append(word)
                            }
                        }
                        word in types || lowerWord in types -> {
                            withStyle(SpanStyle(color = SyntaxTheme.Type, fontWeight = FontWeight.Medium)) {
                                append(word)
                            }
                        }
                        isFunc -> {
                            withStyle(SpanStyle(color = SyntaxTheme.Function)) {
                                append(word)
                            }
                        }
                        word.first().isUpperCase() && word.length > 1 -> {
                            // Probable Class or Interface name
                            withStyle(SpanStyle(color = SyntaxTheme.Type)) {
                                append(word)
                            }
                        }
                        else -> {
                            withStyle(SpanStyle(color = SyntaxTheme.DefaultText)) {
                                append(word)
                            }
                        }
                    }
                    continue
                }

                // 8. Operators and Punctuation
                when (c) {
                    '{', '}', '(', ')', '[', ']', ';', ',', '.' -> {
                        withStyle(SpanStyle(color = SyntaxTheme.Punctuation)) {
                            append(c)
                        }
                    }
                    '=', '+', '-', '*', '/', '%', '&', '|', '^', '!', '<', '>', '?', ':', '~' -> {
                        withStyle(SpanStyle(color = SyntaxTheme.Operator)) {
                            append(c)
                        }
                    }
                    else -> {
                        withStyle(SpanStyle(color = SyntaxTheme.DefaultText)) {
                            append(c)
                        }
                    }
                }
                i++
            }
        }
    }

    private fun highlightJson(code: String): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            val len = code.length

            while (i < len) {
                val c = code[i]

                // Strings
                if (c == '"') {
                    var j = i + 1
                    var escaped = false
                    while (j < len) {
                        val curr = code[j]
                        if (escaped) {
                            escaped = false
                        } else if (curr == '\\') {
                            escaped = true
                        } else if (curr == '"') {
                            j++
                            break
                        } else if (curr == '\n') {
                            break
                        }
                        j++
                    }
                    val str = code.substring(i, j)

                    // Check if followed by colon -> JSON Key
                    var peek = j
                    while (peek < len && code[peek].isWhitespace()) peek++
                    val isKey = peek < len && code[peek] == ':'

                    if (isKey) {
                        withStyle(SpanStyle(color = SyntaxTheme.Variable, fontWeight = FontWeight.SemiBold)) {
                            append(str)
                        }
                    } else {
                        withStyle(SpanStyle(color = SyntaxTheme.String)) {
                            append(str)
                        }
                    }
                    i = j
                    continue
                }

                // Numbers
                if (c.isDigit() || (c == '-' && i + 1 < len && code[i + 1].isDigit())) {
                    val start = i
                    i++
                    while (i < len && (code[i].isDigit() || code[i] == '.' || code[i] == 'e' || code[i] == 'E' || code[i] == '+' || code[i] == '-')) {
                        i++
                    }
                    withStyle(SpanStyle(color = SyntaxTheme.Number)) {
                        append(code.substring(start, i))
                    }
                    continue
                }

                // Booleans / null
                if (c.isLetter()) {
                    val start = i
                    while (i < len && code[i].isLetter()) i++
                    val word = code.substring(start, i)
                    if (word in BOOLEANS) {
                        withStyle(SpanStyle(color = SyntaxTheme.Boolean, fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                    } else {
                        withStyle(SpanStyle(color = SyntaxTheme.DefaultText)) {
                            append(word)
                        }
                    }
                    continue
                }

                // Punctuation
                if (c in "{}[],:") {
                    withStyle(SpanStyle(color = SyntaxTheme.Punctuation)) {
                        append(c)
                    }
                } else {
                    withStyle(SpanStyle(color = SyntaxTheme.DefaultText)) {
                        append(c)
                    }
                }
                i++
            }
        }
    }

    private fun highlightXml(code: String): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            val len = code.length

            while (i < len) {
                // Comments <!-- ... -->
                if (i + 3 < len && code.substring(i, i + 4) == "<!--") {
                    val end = code.indexOf("-->", i + 4)
                    val commentEnd = if (end == -1) len else end + 3
                    withStyle(SpanStyle(color = SyntaxTheme.Comment, fontStyle = FontStyle.Italic)) {
                        append(code.substring(i, commentEnd))
                    }
                    i = commentEnd
                    continue
                }

                // Tags <tag or </tag
                if (code[i] == '<') {
                    withStyle(SpanStyle(color = SyntaxTheme.Tag)) {
                        append('<')
                    }
                    i++
                    if (i < len && code[i] == '/') {
                        withStyle(SpanStyle(color = SyntaxTheme.Tag)) {
                            append('/')
                        }
                        i++
                    }
                    // Tag name
                    val start = i
                    while (i < len && !code[i].isWhitespace() && code[i] != '>' && code[i] != '/') {
                        i++
                    }
                    withStyle(SpanStyle(color = SyntaxTheme.Tag, fontWeight = FontWeight.Bold)) {
                        append(code.substring(start, i))
                    }
                    continue
                }

                // Attributes & Strings inside tags
                if (code[i] == '"' || code[i] == '\'') {
                    val quote = code[i]
                    var j = i + 1
                    while (j < len && code[j] != quote && code[j] != '\n') j++
                    if (j < len && code[j] == quote) j++
                    withStyle(SpanStyle(color = SyntaxTheme.String)) {
                        append(code.substring(i, j))
                    }
                    i = j
                    continue
                }

                if (code[i] == '=' && i > 0 && code[i - 1].isLetterOrDigit()) {
                    withStyle(SpanStyle(color = SyntaxTheme.Operator)) {
                        append('=')
                    }
                    i++
                    continue
                }

                if (code[i] == '>' || (code[i] == '/' && i + 1 < len && code[i + 1] == '>')) {
                    withStyle(SpanStyle(color = SyntaxTheme.Tag)) {
                        append(code[i])
                    }
                    i++
                    continue
                }

                withStyle(SpanStyle(color = SyntaxTheme.DefaultText)) {
                    append(code[i])
                }
                i++
            }
        }
    }

    private fun highlightSql(code: String): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            val len = code.length

            while (i < len) {
                val c = code[i]

                // Comments (-- or /* ... */)
                if (c == '-' && i + 1 < len && code[i + 1] == '-') {
                    val end = code.indexOf('\n', i)
                    val commentEnd = if (end == -1) len else end
                    withStyle(SpanStyle(color = SyntaxTheme.Comment, fontStyle = FontStyle.Italic)) {
                        append(code.substring(i, commentEnd))
                    }
                    i = commentEnd
                    continue
                }

                if (c == '/' && i + 1 < len && code[i + 1] == '*') {
                    val end = code.indexOf("*/", i + 2)
                    val commentEnd = if (end == -1) len else end + 2
                    withStyle(SpanStyle(color = SyntaxTheme.Comment, fontStyle = FontStyle.Italic)) {
                        append(code.substring(i, commentEnd))
                    }
                    i = commentEnd
                    continue
                }

                // Strings ('...')
                if (c == '\'') {
                    var j = i + 1
                    while (j < len) {
                        if (code[j] == '\'') {
                            j++
                            break
                        }
                        j++
                    }
                    withStyle(SpanStyle(color = SyntaxTheme.String)) {
                        append(code.substring(i, j))
                    }
                    i = j
                    continue
                }

                // Numbers
                if (c.isDigit()) {
                    val start = i
                    while (i < len && (code[i].isDigit() || code[i] == '.')) i++
                    withStyle(SpanStyle(color = SyntaxTheme.Number)) {
                        append(code.substring(start, i))
                    }
                    continue
                }

                // Words
                if (c.isLetter() || c == '_') {
                    val start = i
                    while (i < len && (code[i].isLetterOrDigit() || code[i] == '_')) i++
                    val word = code.substring(start, i)
                    val lower = word.lowercase()
                    if (lower in SQL_KEYWORDS) {
                        withStyle(SpanStyle(color = SyntaxTheme.Keyword, fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                    } else {
                        withStyle(SpanStyle(color = SyntaxTheme.Variable)) {
                            append(word)
                        }
                    }
                    continue
                }

                when (c) {
                    ',', ';', '(', ')' -> withStyle(SpanStyle(color = SyntaxTheme.Punctuation)) { append(c) }
                    '=', '<', '>', '!', '+', '-', '*', '/' -> withStyle(SpanStyle(color = SyntaxTheme.Operator)) { append(c) }
                    else -> withStyle(SpanStyle(color = SyntaxTheme.DefaultText)) { append(c) }
                }
                i++
            }
        }
    }
}

/**
 * Modern IDE-Style Code Block Composable with:
 * - Syntax highlighting using [SyntaxHighlighter]
 * - Line numbers gutter with fixed alignment
 * - Language pill header with line count
 * - Copy-to-clipboard action with visual feedback
 * - Horizontal scroll preservation
 */
@Composable
fun CodeBlockView(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isCodeCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCodeCopied) {
        if (isCodeCopied) {
            delay(2000)
            isCodeCopied = false
        }
    }

    val displayLang = remember(language) {
        val clean = language.trim()
        if (clean.isNotBlank() && clean != "code") clean.uppercase() else "CODE"
    }

    // Clean leading/trailing empty lines while keeping internal indentation
    val cleanedCode = remember(code) {
        code.trim('\r', '\n')
    }

    val linesCount = remember(cleanedCode) {
        cleanedCode.lines().size
    }

    val highlightedCode = remember(cleanedCode, language) {
        SyntaxHighlighter.highlight(cleanedCode, language)
    }

    val lineNumbersText = remember(linesCount) {
        (1..linesCount).joinToString("\n")
    }

    val coroutineScope = rememberCoroutineScope()
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SyntaxTheme.Background,
        border = BorderStroke(1.dp, SyntaxTheme.Border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SyntaxTheme.HeaderBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Language badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF32323E)
                    ) {
                        Text(
                            text = displayLang,
                            color = Color(0xFF9CDCFE),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (linesCount > 1) {
                        Text(
                            text = "$linesCount lines",
                            color = Color(0xFF8B949E),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (horizontalScrollState.value > 0) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    horizontalScrollState.animateScrollTo(0)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = "◀ Start",
                                color = Color(0xFF9CDCFE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Copy button
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code", cleanedCode))
                            isCodeCopied = true
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isCodeCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = if (isCodeCopied) Color(0xFF81C784) else Color(0xFFC9D1D9),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCodeCopied) "Copied" else "Copy",
                            color = if (isCodeCopied) Color(0xFF81C784) else Color(0xFFC9D1D9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Code Content + Line Numbers with synchronized scrolling

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(verticalScrollState)
                    .padding(vertical = 8.dp)
            ) {
                // Line numbers gutter for multi-line code
                if (linesCount > 1) {
                    Box(
                        modifier = Modifier
                            .background(SyntaxTheme.GutterBg)
                            .padding(start = 8.dp, end = 8.dp)
                    ) {
                        Text(
                            text = lineNumbersText,
                            color = SyntaxTheme.LineNumber,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.End
                        )
                    }
                    // Vertical separator
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height((linesCount * 18).dp)
                            .background(SyntaxTheme.GutterDivider)
                    )
                }

                // Horizontally scrollable code text with proper Row weight and no soft wrap
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    Text(
                        text = highlightedCode,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp,
                        softWrap = false
                    )
                }
            }
        }
    }
}
