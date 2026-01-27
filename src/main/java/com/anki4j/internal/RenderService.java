package com.anki4j.internal;

import com.anki4j.model.Card;
import com.anki4j.model.Model;
import com.anki4j.model.Note;
import com.anki4j.model.Template;
import com.anki4j.renderer.RenderedCard;
import com.anki4j.renderer.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class RenderService {
    private static final Logger logger = LoggerFactory.getLogger(RenderService.class);

    private final NoteRepository noteRepository;
    private final ModelService modelService;
    private final Renderer renderer;

    public RenderService(NoteRepository noteRepository, ModelService modelService) {
        logger.info("Initializing RenderService");
        this.noteRepository = noteRepository;
        this.modelService = modelService;
        this.renderer = new Renderer();
    }

    public Optional<RenderedCard> renderCard(Card card) {
        logger.info("Rendering card ID: {} (ordinal: {})", card.getId(), card.getOrdinal());
        Optional<Note> noteOpt = noteRepository.getNoteFromCard(card.getId());
        if (noteOpt.isEmpty()) {
            logger.warn("Could not find note for card ID: {}", card.getId());
            return Optional.empty();
        }
        Note note = noteOpt.get();

        Optional<Model> modelOpt = modelService.getModel(note.getModelId());
        if (modelOpt.isEmpty()) {
            logger.warn("Could not find model ID {} for note ID {}", note.getModelId(), note.getId());
            return Optional.empty();
        }
        Model model = modelOpt.get();

        int ord = (int) card.getOrdinal();
        Template template = null;
        if (model.getTmpls() != null && ord < model.getTmpls().size()) {
            template = model.getTmpls().get(ord);
        }

        if (template == null) {
            logger.warn("Could not find template for ordinal {} in model {}", ord, model.getName());
            return Optional.empty();
        }

        logger.info("Successfully rendered card ID: {}", card.getId());
        return Optional.of(renderer.renderCard(note, model, template));
    }
}
