package com.vericore.core.exceptions

sealed class CodeContextException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class ConfigurationException(message: String, cause: Throwable? = null) : CodeContextException(message, cause)

class AIProviderException(message: String, cause: Throwable? = null) : CodeContextException(message, cause)

class AnalysisException(message: String, cause: Throwable? = null) : CodeContextException(message, cause)
