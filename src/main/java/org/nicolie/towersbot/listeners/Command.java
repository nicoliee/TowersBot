package org.nicolie.towersbot.listeners;

import org.nicolie.towersbot.TowersBot;
import org.nicolie.towersbot.database.SQLDatabaseConnection;
import org.nicolie.towersbot.enums.Stat;
import org.nicolie.towersbot.stats.PlayerStats;
import org.nicolie.towersbot.utils.AddButtons;
import org.nicolie.towersbot.utils.ListBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.*;
public class Command extends ListenerAdapter {
    private final SQLDatabaseConnection connection;
    public Command(SQLDatabaseConnection connection) {
        this.connection = connection;
    }
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (!e.getName().equals("towers"))
            return;
        if (e.getMember().getUser().isBot())
            return;
        e.deferReply().queue();
        String subComando = e.getSubcommandName();
        switch (Objects.requireNonNull(subComando)) {
            case "stats": {
                String nombre;
                try {
                    nombre = e.getOption("nombre").getAsString();
                } catch (NullPointerException ex) {
                    e.getHook().sendMessage("Introduce un nombre.").queue();
                    break;
                }
                
                String tabla;
                try {
                    tabla = e.getOption("stats").getAsString();
                } catch (NullPointerException ex) {
                    e.getHook().sendMessage("Introduce una tabla.").queue();
                    break;
                }
                
                if (!connection.hasAccount(nombre, tabla)) {
                    e.getHook().sendMessage("Introduce un nombre válido.").queue();
                    break;
                }
                
                PlayerStats playerStats = new PlayerStats(connection, nombre, tabla);
                EmbedBuilder eb = buildStatsEmbed("Stats The Towers", "https://mc-heads.net/avatar/" + nombre, playerStats, tabla);
                e.getHook().sendMessage(MessageCreateData.fromEmbeds(eb.build())).queue();
                break;
            }
            // Otros casos pueden ir aquí...
            default: {
                e.getHook().sendMessage("Comando no reconocido.").queue();
                break;
            }
            case "top": {
                    String tabla;
                    try {
                        tabla = e.getOption("stats").getAsString();
                    } catch (NullPointerException ex) {
                        e.getHook().sendMessage("Introduce una tabla.").queue();
                        break;
                    }
            
                    final int numElem = getInt(e, "elementos", 10);
                    if (numElem < 1) {
                        e.getHook().sendMessage("Se necesitan más elementos por página.").queue();
                        break;
                    }
            
                    final int pagina = getInt(e, "pagina", 1) - 1;
                    if (pagina < 0) {
                        e.getHook().sendMessage("Introduce una página válida.").queue();
                        break;
                    }
            
                    final Stat stat = getStat(e, "categoria", Stat.RANK);
            
                    final boolean incluirNoFiables = !(!(e.getOption("solo_fiables") == null) && e.getOption("solo_fiables").getAsString().equals("si"));
            
                    final PlayerStats[] list = ListBuilder.getList(connection, stat, incluirNoFiables, tabla);
            
                    final int pages = (int) Math.ceil(list.length / (float) numElem);
                    if (pagina >= pages) {
                        e.getHook().sendMessage("No existe esa página.").queue();
                        break;
                    }
            
                    final boolean hasRanks = (!(e.getOption("mostrar_rangos") == null) && e.getOption("mostrar_rangos").getAsString().equals("si"));
            
                    Map.Entry<String, Integer> paginaDeLista = ListBuilder.buildList(numElem, pagina, incluirNoFiables, stat, hasRanks, list);
            
                    List<Button> buttons = AddButtons.createButtons(incluirNoFiables, hasRanks, pagina, pages, numElem, stat, tabla);
            
                    EmbedBuilder eb;
                    try {
                        eb = new EmbedBuilder().addField("Top " + (pagina * numElem + 1) + "-" + paginaDeLista.getValue() + " " + stat.getText().replace("_", " ") + " " + (TowersBot.ALL_TABLES.equals(tabla) ? "" : tabla), paginaDeLista.getKey(), true);
                    } catch (IllegalArgumentException ex) {
                        e.getHook().sendMessage("Hay demasiados elementos a devolver.").queue();
                        break;
                    }
                    String fiable = incluirNoFiables ? "\n*: Poco fiable, pocos datos." : "";
                    eb.setFooter("Página " + (pagina + 1) + "/" + pages + fiable);
            
                    MessageCreateData message = new MessageCreateBuilder().addEmbeds(eb.build()).addActionRow(buttons).build();
                    e.getHook().sendMessage(message).queue();
                    break;
            }
        }
    }
    private EmbedBuilder buildStatsEmbed(String title, String URL, PlayerStats playerStats, String table) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title + (TowersBot.ALL_TABLES.equals(table) ? "" : " - Partidas " + table)).setThumbnail(URL).addField("Usuario", playerStats.nombre, true);
        HashMap<Stat, Double> stats = playerStats.getStats();
        String format;
        double value;
        String percentage;
        for (Stat st : Stat.getValuesWithout(Stat.RANK)) {
            format = st.isInteger() ? "%.0f" : "%.2f";
            value = stats.get(st);
            percentage = "";
            if (st.isPercentage()) {
                value *= 100;
                percentage = "%";
            }
            eb.addField(st.getEmoji() + st.getText().replace('_', ' '), String.format(format, value) + percentage, true);
        }
        if (playerStats.statsFiables)
            eb.addField("RANGO", playerStats.rank.getAnimatedEmoji() + " (" + String.format("%.2f", playerStats.getStats().get(Stat.RANK)) + ")", true);
        else
            eb.addField("RANGO", playerStats.rank.getAnimatedEmoji() + "*" + " (" + String.format("%.2f", playerStats.getStats().get(Stat.RANK)) + ")", true)
                .setFooter("*: Poco fiable, pocos datos.");
        return eb;
    }
    private int getInt(SlashCommandInteractionEvent e, String dataName, int defaultValue) {
        return e.getOption(dataName) == null ? defaultValue : Integer.parseInt(e.getOption(dataName).getAsString());
    }
    private Stat getStat(SlashCommandInteractionEvent e, String dataName, Stat defaultValue) {
        return e.getOption(dataName) == null ? defaultValue : Stat.getStatFromText(e.getOption(dataName).getAsString());
    }
}
