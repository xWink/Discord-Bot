/**
 * The classes in this package represent the connections between
 * the bot and each table it utilizes in the database. These classes
 * contain methods which simplify the usage of all commands that
 * require access to the database. Connector is the abstraction of
 * the connection between the database and the bot and all Connector
 * classes must extend it. These connectors are freely accessed by
 * any command that needs them.
 */
package database;
