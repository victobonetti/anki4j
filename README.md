# Anki4j

**Anki4j** is a robust, lightweight Java library designed to seamlessly parse and extract data from Anki deck files (`.apkg`). Built with performance and modern Java applications in mind, it provides a simple API to access your Decks, Cards, and Notes without managing complex file operations or database connections manually.

It is optimized for Desktop and Server-side Java applications, with full compatibility for **GraalVM** and **Quarkus** native compilation.

## 🚀 Key Features

-   **Zero-Config Extraction**: Automatically handles `.apkg` unzipping and temporary file management.
-   **Universal Support**: Reads both modern Anki versions (decks table) and legacy exports (JSON in col table).
-   **Safe Resource Management**: Implements `AutoCloseable` to ensure temporary files and database connections are always cleaned up.
-   **Pure Java**: No native dependencies beyond the embedded SQLite driver. Java 21+ ready.

## 📦 Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.anki4j</groupId>
    <artifactId>anki4j</artifactId>
    <version>0.0.1</version>
</dependency>
```

## 📖 Usage

Using Anki4j is as simple as opening a file. The library handles the rest.

### Enhanced Data Navigation (Lazy Loading)

Anki4j supports efficient, easy-to-use navigation through your Decks, Cards, and Notes.

```java
try (Anki4j anki = Anki4j.read("/path/to/my-deck.apkg")) {
    
    // Get all Decks
    List<Deck> decks = anki.getDecks();
    
    for (Deck deck : decks) {
        System.out.println("Deck: " + deck.getName());
        
        // Lazy load cards in this deck
        List<Card> cards = deck.getCards();
        System.out.println("  Contains " + cards.size() + " cards.");
        
        for (Card card : cards) {
            // Lazy load the note for this card
            java.util.Optional<Note> noteOpt = card.getNote();
            noteOpt.ifPresent(note -> {
                // Use helper methods to access Front (Title) and Back (Content)
                System.out.println("    Front: " + note.getTitle());
                System.out.println("    Back:  " + note.getContent());
                
                // Access media
                if (!note.getMediaReferences().isEmpty()) {
                    System.out.println("    Media: " + note.getMediaReferences());
                }

                Optional<byte[]> mediaBytes = anki.getMediaContent(note.getMediaReferences().get(0));

                mediaBytes.ifPresent(bytes -> {
                    System.out.println("Media read test: " + bytes.length);
                });
            });
        }
    }

} catch (Exception e) {
    e.printStackTrace();
}
```

## 🛠 Tech Stack

-   **Java 21**
-   **SQLite JDBC**: For direct access to the Anki database structure.
-   **Jackson**: For parsing legacy JSON-encoded deck configurations.
-   **SLF4J**: For unified logging.

## 📚 Data Dictionary

### Deck
Represents a collection of cards.
| Field | Type | Description |
|---|---|---|
| `id` | `long` | Unique identifier (epoch timestamp in older versions, random in newer). |
| `name` | `String` | The name of the deck (e.g., "Japanese::Vocabulary"). |
| `getCards()` | `List<Card>` | Lazy-loaded list of cards belonging to this deck. |

### Card
Represents a single flashcard (a specific facet of a note).
| Field | Type | Description |
|---|---|---|
| `id` | `long` | Unique identifier for the card. |
| `noteId` | `long` | ID of the source Note (the content container). |
| `deckId` | `long` | ID of the Deck this card currently resides in. |
| `ordinal` | `long` | Index of the card template (0 for first card type, 1 for second, etc.). Identifies which "side" or variant of the note this card represents. |
| `getNote()` | `Optional<Note>` | Lazy-loaded reference to the parent Note. |

### Note
Represents the core data entry (the "fact") independent of how it's displayed.
| Field | Type | Description |
|---|---|---|
| `id` | `long` | Unique identifier (often creation timestamp). |
| `guid` | `String` | Globally Unique ID used for syncing. |
| `modelId` | `long` | ID of the Note Type (Model) that defines schema/fields. |
| `fields` | `String` | Raw string concatenation of all field values, separated by `0x1F` (Unit Separator). |
| `getTitle()`| `String` | Convenience method to get the first field (usually "Front"). |
| `getContent()`| `String` | Convenience method to get the second field (usually "Back"). |
