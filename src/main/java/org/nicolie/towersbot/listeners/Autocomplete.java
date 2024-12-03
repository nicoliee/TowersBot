package org.nicolie.towersbot.listeners;

import org.nicolie.towersbot.database.SQLDatabaseConnection;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Autocomplete extends ListenerAdapter {
    private final String[] playerNames;
    public Autocomplete(SQLDatabaseConnection connection) {
        playerNames = connection.getPlayerNames();
    }
    
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent e) {
        if (!e.getName().equals("towers") || !e.getFocusedOption().getName().equals("nombre"))
            return;

        // Evitar duplicados verificando si ya se ha respondido
        if (e.isAcknowledged()) {
            return; // No hacer nada si ya ha sido reconocido
        }

        List<Command.Choice> options = Stream.of(playerNames)
                .filter(word -> word.regionMatches(true, 0, e.getFocusedOption().getValue(), 0, e.getFocusedOption().getValue().length()))
                .limit(25) // Solo mostrar las palabras que empiecen con el input del usuario
                .map(word -> new Command.Choice(word, word)) // Mapear las palabras a opciones
                .collect(Collectors.toList());

        // Responder a la interacción
        e.replyChoices(options).queue();
    }
}