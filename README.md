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
            Note note = card.getNote();
            if (note != null) {
                String[] fields = note.getFieldParts();
                if (fields.length > 0) {
                     System.out.println("    Front: " + fields[0]);
                }
            }
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
