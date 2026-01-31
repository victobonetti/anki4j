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

## ✍️ Editing and Persistence

Anki4j allows you to modify note fields and persist them back to the original `.apkg` file.

```java
try (AnkiCollection anki = AnkiCollection.read("my-deck.apkg")) {
    Note note = anki.getNote(12345).get();
    
    // Use getFieldsMap() for easy name-based editing
    NoteFieldsMap fields = note.getFieldsMap();
    fields.set("Front", "Updated Question");
    fields.set("Back", "Updated Answer");
    
    // Save the changes to the internal database
    anki.save(note);
    
} // The .apkg file is updated automatically when 'close()' is called
```

> [!WARNING]
> The `.apkg` file is only overwritten on `close()` if `anki.save(note)` was called. Changes are initially applied to a temporary database.

## 🏗️ Creating New Collections

You can create a new Anki collection from scratch, populate it, and export it as an `.apkg` file.

```java
try (AnkiCollection anki = Anki4j.create()) {
    // 1. Define and add a Model
    Model model = new Model();
    model.setId(123456L);
    model.setName("Basic Model");
    // ... setup fields and templates ...
    anki.addModel(model);

    // 2. Add a Deck
    Deck deck = new Deck(1L, "Default");
    anki.addDeck(deck);

    // 3. Add a Note (GUID is generated automatically if null)
    Note note = new Note(10L, null, "Front\u001fBack", model.getId());
    anki.addNote(note);

    // 4. Add a Card linked to the note and deck
    Card card = new Card(1000L, 10L, 1L, 0);
    anki.addCard(card);

    // 5. Add Media
    anki.addMedia("image.png", bytes);

    // 6. Export to APKG
    byte[] apkgBytes = anki.export();
    
} // Resources cleaned up automatically
```

## ⚙️ Configuration

Anki4j has a safety limit for the maximum size of `.apkg` files it will process to prevent excessive memory usage.

| Variable | Default | Description |
|----------|---------|-------------|
| `ANKI4J_MAX_PKG_SIZE_KB` | `1000` (1MB) | Maximum allowed size for the entire `.apkg` file in Kilobytes. |

If you need to process larger decks, set this environment variable before running your application:

```bash
export ANKI4J_MAX_PKG_SIZE_KB=50000 # Allow up to 50MB
```

## API Reference

### AnkiCollection (Interface)

| Method | Returns | Description |
|--------|---------|-------------|
| `read(String path)` | `AnkiCollection` | Static factory to open an `.apkg` file |
| `create()` | `AnkiCollection` | Static factory to create a new empty collection |
| `getDecks()` | `List<Deck>` | All decks in the collection |
| `getDeck(long id)` | `Optional<Deck>` | Deck by ID |
| `getCards(long deckId)` | `List<Card>` | Cards in a deck |
| `getCard(long id)` | `Optional<Card>` | Card by ID |
| `getNote(long id)` | `Optional<Note>` | Note by ID |
| `getModel(long id)` | `Optional<Model>` | Model by ID |
| `getMediaContent(String name)` | `Optional<byte[]>` | Media file bytes |
| `renderCard(Card card)` | `Optional<RenderedCard>` | Rendered card content |
| `save(Note note)` | `void` | Marks an existing note for persistence |
| `addDeck(Deck deck)` | `void` | Adds a new deck to the collection |
| `addModel(Model model)` | `void` | Adds a new model to the collection |
| `addNote(Note note)` | `void` | Adds a new note to the collection |
| `addCard(Card card)` | `void` | Adds a new card to the collection |
| `addMedia(String name, byte[] data)`| `void` | Adds a new media file to the collection |
| `export()` | `byte[]` | Exports the collection as an APKG file |

### Domain Objects

**Deck** - Collection of cards
- `getId()`, `getName()`, `getCards()` (lazy-loaded)

**Card** - Single flashcard instance
- `getId()`, `getNoteId()`, `getDeckId()`, `getOrdinal()`, `getNote()` (lazy-loaded)

**Note** - Core data entry
- `getId()`, `getGuid()`, `getModelId()`, `getFields()`, `getFieldsMap()`, `getMediaReferences()`

**Model** - Note type schema
- `getId()`, `getName()`, `getFlds()`, `getTmpls()`, `getCss()`

**RenderedCard** - Rendered output
- `getFields()`, `getFront()`, `getBack()`, `getCss()`

## 🛠 Tech Stack

- **Java 21**
- **SQLite JDBC** - Database access
- **Jackson** - JSON parsing
- **SLF4J** - Logging
