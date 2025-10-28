# clj-info

[![Clojars Project](https://img.shields.io/clojars/v/clj-info.svg)](https://clojars.org/clj-info)
[![GitHub](https://img.shields.io/github/license/franks42/clj-info.svg)](LICENSE)
[![Clojure](https://img.shields.io/badge/clojure-1.12.1-blue.svg)](https://clojure.org/)
[![Babashka](https://img.shields.io/badge/babashka-compatible-green.svg)](https://babashka.org/)

An enhanced Clojure documentation facility that provides **better docs than `doc`** with richer formatting, multiple output formats, and comprehensive Babashka support.

**clj-info** modernizes Clojure documentation with:
- 🎨 **Rich ANSI terminal formatting** with colors and structured layout
- 📝 **Markdown export** for documentation systems  
- 🔄 **JSON/EDN output** for programmatic consumption
- 🚀 **Full Babashka compatibility** via SCI (Small Clojure Interpreter)
- 📋 **Enhanced information** beyond standard `doc` function
- 🔧 **Extensible design** with protocol-based architecture

## Quick Start

### Installation

**Leiningen/Boot:**
```clojure
[clj-info "0.5.1"]
```

**Clojure CLI/deps.edn:**
```clojure
clj-info/clj-info {:mvn/version "0.5.1"}
```

**Babashka bb.edn:**
```clojure
{:deps {clj-info/clj-info {:mvn/version "0.5.1"}}}
```

### Basic Usage

```clojure
(require '[clj-info :as ci])

;; Basic documentation (enhanced version of `doc`)
(ci/info map)
(ci/info reduce)
(ci/info String)

;; Function version for programmatic use
(ci/info* 'filter)
(ci/info* 'clojure.core/assoc)
```

## Output Formats

### 1. Rich Terminal Output (Default)
Beautiful ANSI-colored output with structured formatting:

```clojure
(require '[clj-info :as ci]
         '[clj-info.doc2rich :as rich]
         '[clj-info.doc2map :as doc-map])

;; Rich formatted output with colors and borders
(print (rich/doc->rich (doc-map/get-docs-map 'map)))
```

### 2. Markdown Export
Perfect for documentation systems, wikis, and README files:

```clojure
(require '[clj-info.doc2md :as md])

;; Generate Markdown
(md/doc->md (doc-map/get-docs-map 'reduce))

;; Save to file
(spit "function-docs.md" (md/doc->md (doc-map/get-docs-map 'map)))
```

### 3. JSON/EDN Data Export
Structured data for tooling and programmatic consumption:

```clojure
(require '[clj-info.doc2data :as data])

;; JSON export
(data/doc->json (doc-map/get-docs-map 'filter))

;; EDN export  
(data/doc->edn (doc-map/get-docs-map 'assoc))
```

## Babashka Support

**clj-info** works seamlessly in both JVM Clojure and Babashka environments:

```bash
# Start interactive Babashka nREPL server with clj-info loaded
bb bb_nrepl_server.clj

# Or use directly in Babashka scripts
bb -e "(require '[clj-info :as ci]) (ci/info map)"
```

The library automatically detects the runtime environment and adapts accordingly:
- **Platform detection**: `clj-info.platform/bb?`
- **SCI compatibility**: Full protocol extensions for `sci.lang.Var`
- **Graceful fallbacks**: Features degrade gracefully when dependencies aren't available

## Features

### Enhanced Documentation
- **Comprehensive metadata**: Arglists, docstrings, source info, and more
- **Java class support**: Documentation for Java classes and methods
- **Namespace information**: Complete namespace details and public vars
- **Protocol and multimethod support**: Enhanced info for advanced Clojure constructs

### Modern Output Formats
- **ANSI colors**: Syntax highlighting and visual structure in terminals
- **Unicode borders**: Clean, professional-looking output formatting  
- **Structured tables**: Organized presentation of complex information
- **Multiple export formats**: Text, Markdown, JSON, EDN, and HTML

### Cross-Platform Compatibility
- **JVM Clojure**: Full feature support with all dependencies
- **Babashka**: Complete compatibility via SCI extensions
- **Graceful degradation**: Works even when optional dependencies are missing

## API Reference

### Core Functions
- `(info symbol)` - Macro version, no quoting needed
- `(info* 'symbol)` - Function version for programmatic use  
- `(tdoc symbol)` - Text documentation (legacy compatibility)

### Specialized Output
- `(rich/doc->rich doc-map)` - Rich ANSI terminal formatting
- `(md/doc->md doc-map)` - Markdown generation
- `(data/doc->json doc-map)` - JSON export
- `(data/doc->edn doc-map)` - EDN export

### Core Data
- `(doc-map/get-docs-map 'symbol)` - Raw documentation map

## Examples

### Exploring Clojure Core Functions
```clojure
;; Rich terminal output  
(ci/info map)
(ci/info reduce)
(ci/info filter)

;; Compare with standard doc
(doc map)     ; Plain text
(ci/info map) ; Rich formatted
```

### Documentation Export Workflow
```clojure
;; Generate documentation for multiple functions
(require '[clj-info.doc2md :as md]
         '[clj-info.doc2map :as doc-map])

(defn export-function-docs [functions filename]
  (->> functions
       (map #(md/doc->md (doc-map/get-docs-map %)))
       (clojure.string/join "\n\n---\n\n")
       (spit filename)))

(export-function-docs '[map reduce filter assoc] "core-functions.md")
```

### JSON API Integration
```clojure
;; Generate JSON documentation for API consumption
(require '[clj-info.doc2data :as data])

(defn api-doc-endpoint [function-name]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (data/doc->json (doc-map/get-docs-map (symbol function-name)))})
```

## Development

### Running Tests
```bash
# JVM Clojure
lein test

# Babashka
bb test_bb.clj
```

### Interactive Development
```bash
# Start nREPL server with clj-info loaded
bb bb_nrepl_server.clj

# Connect with your favorite editor/client
lein repl :connect 7888
```

## Version History

### 0.5.1 (2024-10-28) - ClojureDocs Integration & Documentation
- ✅ **Comprehensive ClojureDocs integration** across all output formats
- ✅ **Complete documentation overhaul** with modern README and changelog
- ✅ **Interactive development tools** including Babashka nREPL server
- ✅ **Cross-format consistency** for reference links and URLs

### 0.5.0 (2024-10-28) - Major Modernization Release
- ✅ **Full Babashka compatibility** via SCI extensions
- ✅ **Rich ANSI terminal formatting** with colors and borders
- ✅ **Markdown export** for modern documentation workflows  
- ✅ **JSON/EDN data export** for programmatic consumption
- ✅ **Updated dependencies**: Clojure 1.12.1, Hiccup 2.0.0, latest libraries
- ✅ **Platform detection system** for graceful cross-environment operation
- ✅ **Protocol-based architecture** for extensibility

### 0.4.2 and earlier
- Legacy versions with basic text output and HTML generation

## Compatibility

- **Clojure**: 1.12.1+ (works with earlier versions)
- **Babashka**: Latest versions with SCI support
- **JVM**: Java 8+
- **Dependencies**: All optional with graceful fallbacks

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality  
4. Ensure both JVM and Babashka compatibility
5. Submit a pull request

## Acknowledgements

- Thanks to the Clojure community for feedback and inspiration
- Special thanks to Babashka/SCI developers for the excellent runtime
- Original inspiration from discussions on the clojure-user mailing list

## License

Copyright (C) 2011-2024 Frank Siebenlist

Distributed under the Eclipse Public License 1.0, the same as Clojure uses. See the file [COPYING](COPYING).
