package org.nicolie.towersbot.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.nicolie.towersbot.TowersBot;

public class TowersBotCommand implements CommandExecutor {
    private final TowersBot plugin;

    public TowersBotCommand(TowersBot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar permisos al inicio
        if (!sender.hasPermission("towers.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        // Verificar que se pasen argumentos
        if (args.length == 0) {
            sender.sendMessage("§cUso: /TowersBot <lista|delete|add|reload>");
            return true;
        }

        FileConfiguration config = plugin.getConfig();

        switch (args[0].toLowerCase()) {
            case "reload":
                sender.sendMessage("§aReiniciando TowersBot...");
                try {
                    plugin.onDisable();
                    plugin.reloadConfig();
                    plugin.onEnable();
                    sender.sendMessage("§aTowersBot ha sido reiniciado correctamente.");
                } catch (Exception e) {
                    sender.sendMessage("§cHubo un error al reiniciar TowersBot: " + e.getMessage());
                    e.printStackTrace();
                }
                return true;

            case "lista":
                String tablas = config.getString("DB_TABLES", "");
                if (tablas.isEmpty()) {
                    sender.sendMessage("§eNo hay tablas configuradas en DB_TABLES.");
                } else {
                    String[] tablasArray = tablas.split(",");
                    sender.sendMessage("§aTablas configuradas:");
                    for (int i = 0; i < tablasArray.length; i++) {
                        sender.sendMessage("§e" + (i + 1) + ". " + tablasArray[i]);
                    }
                }
                return true;

            case "delete":
                if (args.length != 2) {
                    sender.sendMessage("§cUso: /TowersBot delete {numero}");
                    return true;
                }

                try {
                    int index = Integer.parseInt(args[1]) - 1;
                    String tablasDelete = config.getString("DB_TABLES", "");
                    String[] tablasArray = tablasDelete.split(",");

                    if (index < 0 || index >= tablasArray.length) {
                        sender.sendMessage("§cEl número especificado no es válido.");
                        return true;
                    }

                    String eliminada = tablasArray[index];

                    // Reconstruir la cadena sin la tabla eliminada
                    StringBuilder nuevasTablas = new StringBuilder();
                    for (int i = 0; i < tablasArray.length; i++) {
                        if (i != index) {
                            if (nuevasTablas.length() > 0) {
                                nuevasTablas.append(",");
                            }
                            nuevasTablas.append(tablasArray[i]);
                        }
                    }

                    config.set("DB_TABLES", nuevasTablas.toString());
                    plugin.saveConfig();

                    sender.sendMessage("§aTabla eliminada: " + eliminada);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cEl argumento debe ser un número válido.");
                }
                return true;

            case "add":
                if (args.length != 2) {
                    sender.sendMessage("§cUso: /TowersBot add {tabla}");
                    return true;
                }

                String nuevaTabla = args[1];
                String tablasAdd = config.getString("DB_TABLES", "");

                if (tablasAdd.contains(nuevaTabla)) {
                    sender.sendMessage("§cLa tabla ya está configurada.");
                    return true;
                }

                // Añadir la nueva tabla con coma al final
                if (!tablasAdd.isEmpty()) {
                    tablasAdd += ",";
                }
                tablasAdd += nuevaTabla;

                config.set("DB_TABLES", tablasAdd);
                plugin.saveConfig();

                sender.sendMessage("§aTabla añadida: " + nuevaTabla);
                return true;

            case "help":
                sender.sendMessage("§aComandos disponibles:");
                sender.sendMessage("§e/TowersBot lista §7- Muestra las tablas configuradas.");
                sender.sendMessage("§e/TowersBot delete {numero} §7- Elimina una tabla de la configuración.");
                sender.sendMessage("§e/TowersBot add {tabla} §7- Añade una tabla a la configuración.");
                sender.sendMessage("§e/TowersBot reload §7- Recarga la configuración del plugin.");
                return true;
                
            default:
                sender.sendMessage("§cComando desconocido. Uso: /TowersBot <lista|delete|add|reload>");
                return true;
        }
    }
}
