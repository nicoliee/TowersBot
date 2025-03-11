package org.nicolie.towersbot;

import org.nicolie.towersbot.commands.TowersBotCommand;
import org.nicolie.towersbot.database.SQLDatabaseConnection;
import org.nicolie.towersbot.listeners.Command;
import org.nicolie.towersbot.listeners.Button;
import org.nicolie.towersbot.listeners.Autocomplete;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.io.File;

public final class TowersBot extends JavaPlugin {
    public static final String ALL_TABLES = "ALL_TABLES";
    private SQLDatabaseConnection connection;
    private JDA bot;

    @Override
        public void onEnable() {
        // Registro del comando /TowersBot
        getCommand("TowersBot").setExecutor(new TowersBotCommand(this));

        // Obtener el archivo de configuración de AmazingTowers
        File gameSettingsFile = new File(Bukkit.getServer().getPluginManager()
                .getPlugin("AmazingTowers").getDataFolder(), "globalConfig.yml");

        if (!gameSettingsFile.exists()) {
                getLogger().severe("El archivo globalConfig.yml no existe en AmazingTowers.");
                getServer().getPluginManager().disablePlugin(this);
                return;
        }

        // Cargar la configuración desde globalConfig.yml
        FileConfiguration gameConfig = YamlConfiguration.loadConfiguration(gameSettingsFile);

        // Cargar la configuración desde config.yml de TowersBot
        FileConfiguration mainConfig = getConfig();

        // Leer valores desde config.yml del plugin TowersBot
        String token = mainConfig.getString("DISCORD_TOKEN");
        String dbTables = mainConfig.getString("DB_TABLES");

        // Leer valores de la base de datos desde globalConfig.yml
        String dbHost = gameConfig.getString("options.database.hostname");
        String dbName = gameConfig.getString("options.database.database");
        String dbUser = gameConfig.getString("options.database.user");
        String dbPassword = gameConfig.getString("options.database.password");

        // Verificar que los valores sean válidos
        if (token == null || token.isEmpty()) {
                getLogger().severe("DISCORD_TOKEN no está configurado en config.yml.");
                getServer().getPluginManager().disablePlugin(this);
                return;
        }

        if (dbTables == null || dbTables.isEmpty()) {
                getLogger().severe("DB_TABLES no está configurado en config.yml.");
                getServer().getPluginManager().disablePlugin(this);
                return;
        }

        if (dbHost == null || dbHost.isEmpty() ||
                dbName == null || dbName.isEmpty() ||
                dbUser == null || dbUser.isEmpty() ||
                dbPassword == null || dbPassword.isEmpty()) {
                getLogger().severe("Faltan configuraciones de base de datos en globalConfig.yml.");
                getServer().getPluginManager().disablePlugin(this);
                return;
        }

        // Crear la conexión a la base de datos usando los valores leídos
        connection = new SQLDatabaseConnection(dbHost, dbName, dbUser, dbPassword, dbTables);

        // Conectar a la base de datos
        if (!connection.Conectar()) {
                getLogger().severe("No se pudo conectar a la base de datos. Desactivando plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
        }

        // Crear el bot de Discord
        try {
                bot = JDABuilder.createDefault(token)
                        .setActivity(Activity.playing("The Towers"))
                        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                        .build()
                        .awaitReady();

                registerCommands(bot);
                addListeners(bot);
        } catch (Exception e) {
                getLogger().severe("Error al iniciar el bot de Discord: " + e.getMessage());
                getServer().getPluginManager().disablePlugin(this);
        }
}

    private void registerCommands(JDA bot) {
        List<net.dv8tion.jda.api.interactions.commands.Command.Choice> listOfStatChoices = Arrays.stream(org.nicolie.towersbot.enums.Stat.values())
                .map(org.nicolie.towersbot.enums.Stat::getText)
                .map(x -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(x, x))
                .collect(Collectors.toList()); 

        List<net.dv8tion.jda.api.interactions.commands.Command.Choice> listOfTables = connection.getTables().stream()
                .map(x -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(x, x))
                .collect(Collectors.toList()); 

        bot.updateCommands().addCommands(
                Commands.slash("towers", "Comando The Towers")
                        .addSubcommands(new SubcommandData("stats", "Estadísticas").addOptions(
                                new OptionData(OptionType.STRING, "nombre", "El nombre del jugador", true, true),
                                new OptionData(OptionType.STRING, "stats", "Contar solo amistosas, torneo o las dos", true, false)
                                        .addChoices(listOfTables).addChoice("todos", ALL_TABLES)
                        ))
                        .addSubcommands(new SubcommandData("top", "Muestra a los jugadores ordenados por rango").addOptions(
                                new OptionData(OptionType.STRING, "stats", "Contar solo amistosas, torneo o las dos", true, false)
                                        .addChoices(listOfTables).addChoice("todos", ALL_TABLES),
                                new OptionData(OptionType.STRING, "solo_fiables", "Excluir datos poco fiables o no", false, false)
                                        .addChoices(new net.dv8tion.jda.api.interactions.commands.Command.Choice("si", "si"))
                                        .addChoices(new net.dv8tion.jda.api.interactions.commands.Command.Choice("no", "no")),
                                new OptionData(OptionType.STRING, "categoria", "Estadística con la que ordenar la tabla", false, false)
                                        .addChoices(listOfStatChoices),
                                new OptionData(OptionType.INTEGER, "pagina", "La página de la tabla", false, false),
                                new OptionData(OptionType.STRING, "mostrar_rangos", "Mostrar rango o no", false, false)
                                        .addChoices(new net.dv8tion.jda.api.interactions.commands.Command.Choice("si", "si"))
                                        .addChoices(new net.dv8tion.jda.api.interactions.commands.Command.Choice("no", "no")),
                                new OptionData(OptionType.INTEGER, "elementos", "Número de elementos por página", false, false)
                        ))
        ).queue();
    }

    private void addListeners(JDA bot) {
        bot.addEventListener(new Command(connection));
        bot.addEventListener(new Button(connection));
        bot.addEventListener(new Autocomplete(connection));
    }
}
