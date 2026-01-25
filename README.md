# Anki4j

**Anki4j** is a lightweight Java library for parsing Anki deck files (`.apkg`). It provides a clean API to access Decks, Cards, Notes, and rendered content without manual file or database management.

Optimized for **Java 21+**, with full **GraalVM** and **Quarkus** native compilation support.

## 🚀 Features

- **Zero-Config Extraction**: Handles `.apkg` unzipping and temp file management automatically
- **Universal Support**: Reads both modern Anki (decks table) and legacy exports (JSON in col table)
- **Template Rendering**: Built-in Mustache-like template engine with conditional support
- **Safe Resources**: Implements `AutoCloseable` for automatic cleanup

## 📦 Installation

```xml
<dependency>
    <groupId>com.anki4j</groupId>
    <artifactId>anki4j</artifactId>
    <version>0.0.1</version>
</dependency>
```

## 📖 Quick Start

```java
try (AnkiCollection anki = AnkiCollection.read("/path/to/deck.apkg")) {
    
    for (Deck deck : anki.getDecks()) {
        System.out.println("Deck: " + deck.getName());
        
        for (Card card : deck.getCards()) {
            // Render card content
            anki.renderCard(card).ifPresent(rendered -> {
                System.out.println("Q: " + rendered.getFront());
                System.out.println("A: " + rendered.getBack());
            });
        }
    }
    
} catch (Exception e) {
    e.printStackTrace();
}
```

## 🎯 Structured Rendering

Use `renderCard()` to get a `RenderedCard` with field map, rendered HTML, and CSS:

```java
anki.renderCard(card).ifPresent(rendered -> {
    Map<String, String> fields = rendered.getFields();  // Raw field values
    String question = rendered.getFront();               // Rendered HTML
    String answer = rendered.getBack();
    String css = rendered.getCss();                      // Model CSS
});
```

**Template Syntax:**
| Syntax | Description |
|--------|-------------|
| `{{FieldName}}` | Simple field substitution |
| `{{#Field}}...{{/Field}}` | Render if field is not empty |
| `{{^Field}}...{{/Field}}` | Render if field IS empty |
| `{{FrontSide}}` | Include front in answer template |
| `{{cloze:Field}}` | Cloze deletion |
| `{{hint:Field}}` | Collapsible hint |

## 🏷️ Working with Models

```java
note.getModel().ifPresent(model -> {
    System.out.println("Model: " + model.getName());
    model.getFlds().forEach(f -> System.out.println("  Field: " + f.getName()));
    System.out.println("CSS: " + model.getCss());
});
```

## API Reference

### AnkiCollection (Interface)

| Method | Returns | Description |
|--------|---------|-------------|
| `read(String path)` | `AnkiCollection` | Static factory to open an `.apkg` file |
| `getDecks()` | `List<Deck>` | All decks in the collection |
| `getDeck(long id)` | `Optional<Deck>` | Deck by ID |
| `getCards(long deckId)` | `List<Card>` | Cards in a deck |
| `getCard(long id)` | `Optional<Card>` | Card by ID |
| `getNote(long id)` | `Optional<Note>` | Note by ID |
| `getModel(long id)` | `Optional<Model>` | Model by ID |
| `getMediaContent(String name)` | `Optional<byte[]>` | Media file bytes |
| `renderCard(Card card)` | `Optional<RenderedCard>` | Rendered card content |

### Domain Objects

**Deck** - Collection of cards
- `getId()`, `getName()`, `getCards()` (lazy-loaded)

**Card** - Single flashcard instance
- `getId()`, `getNoteId()`, `getDeckId()`, `getOrdinal()`, `getNote()` (lazy-loaded)

**Note** - Core data entry
- `getId()`, `getGuid()`, `getModelId()`, `getFields()`, `getMediaReferences()`

**Model** - Note type schema
- `getId()`, `getName()`, `getFlds()`, `getTmpls()`, `getCss()`

**RenderedCard** - Rendered output
- `getFields()`, `getFront()`, `getBack()`, `getCss()`

## 🛠 Tech Stack

- **Java 21**
- **SQLite JDBC** - Database access
- **Jackson** - JSON parsing
- **SLF4J** - Logging
