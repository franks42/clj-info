# Changelog

All notable changes to clj-info will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0] - 2024-10-28 - Major Modernization Release

### Added
- **Full Babashka compatibility** via SCI (Small Clojure Interpreter) extensions
- **Rich ANSI terminal formatting** with colors, borders, and structured layout
- **Markdown export functionality** for modern documentation workflows
- **JSON/EDN data export** for programmatic consumption and API integration
- **Platform detection system** (`clj-info.platform` namespace) for graceful cross-environment operation
- **Protocol-based architecture** for extensible documentation rendering
- **nREPL server script** (`bb_nrepl_server.clj`) for interactive testing
- **Comprehensive test coverage** for both JVM and Babashka environments

### Enhanced
- **Core documentation extraction** with improved metadata handling
- **Java class documentation** with platform-aware javadoc URL generation
- **Namespace documentation** with enhanced public var listings
- **Error handling** with graceful fallbacks when dependencies are unavailable

### Updated
- **Clojure dependency**: 1.12.0 → 1.12.1 (latest stable)
- **Hiccup dependency**: 1.0.5 → 2.0.0 (major version update)
- **org.clojure/data.json**: 2.5.0 → 2.5.1 (latest)
- **Project version**: 0.4.2 → 0.5.0
- **README.md**: Completely rewritten with modern examples and comprehensive API documentation

### Fixed
- **SCI var compatibility** for Babashka environments
- **Import dependencies** in utils namespace  
- **Protocol implementations** for `sci.lang.Var` type
- **Reflection warnings** with better type handling

### Technical Improvements
- **Cross-platform JSON encoding** (Cheshire for Babashka, clojure.data.json for JVM)
- **Safe colorization** with automatic ANSI detection and fallbacks
- **Modular architecture** with separate namespaces for each output format
- **Memory-efficient** documentation processing
- **Thread-safe** implementation

### Development
- **Published to Clojars** as version 0.5.0
- **Git tags** with detailed release notes (v0.5.0-beta.1, v0.5.0)
- **Automated deployment** configuration for future releases
- **Development tools** including nREPL server and test scripts

## [0.4.2] and earlier - Legacy Versions

### Features (Historical)
- Basic text documentation enhancement over standard `doc`
- HTML output generation
- Simple extensibility for custom documentation
- Java class and namespace documentation
- Poor-man's localization support

### Dependencies (Historical)
- Clojure 1.x.x (various versions)
- Hiccup 1.0.5
- Basic HTML generation tools

---

## Migration Guide: 0.4.2 → 0.5.0

### Breaking Changes
None - the new version is fully backward compatible.

### New Capabilities
1. **Babashka Support**: Your existing code now works in Babashka without modification
2. **Rich Output**: Add `(require '[clj-info.doc2rich :as rich])` for colorized output
3. **Export Formats**: New namespaces for Markdown (`doc2md`) and JSON/EDN (`doc2data`)
4. **Platform Detection**: Use `clj-info.platform/bb?` to detect runtime environment

### Recommended Updates
```clojure
;; Old usage (still works)
(use 'clj-info)
(tdoc map)

;; New recommended usage
(require '[clj-info :as ci])
(ci/info map)  ; Enhanced with colors and better formatting

;; New capabilities
(require '[clj-info.doc2rich :as rich]
         '[clj-info.doc2map :as doc-map])
(print (rich/doc->rich (doc-map/get-docs-map 'map)))
```

### Dependency Updates
Update your `project.clj` or `deps.edn`:
```clojure
;; Old
[clj-info "0.4.2"]

;; New  
[clj-info "0.5.0"]
```