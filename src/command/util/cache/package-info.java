/**
 * Classes in this package define the queue for Bang updates.
 * BangUpdate is a class containing data about the results of
 * a game of Bang (from a Bang command). These updates are stored
 * in a queue within BangCache, which checks for high frequency of
 * use and activates panic mode once a threshold is reached. During
 * panic mode, the queue will retain data and avoid sending updates
 * to increase speed. Once panic mode ends (usage of Bang slows down),
 * the updates are all sent out at once.
 */

package command.util.cache;
