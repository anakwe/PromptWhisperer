package com.promptwhisperer.models

/**
 * Controls how enhancement memory is read/written across generations.
 */
enum class ContextScope {
    STATELESS,
    SESSION,
    PROJECT,
    GLOBAL,
}

/**
 * Lightweight domain profile inferred from the current request.
 */
enum class DomainProfile {
    SECURITY_TOOL,
    GAME,
    PLUGIN,
    BACKEND_API,
    MOBILE_APP,
    CLI_TOOL,
    AI_AGENT,
    GENERIC,
}
