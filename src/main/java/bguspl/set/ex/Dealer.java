package bguspl.set.ex;

import bguspl.set.Env;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collections;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;
    public AtomicBoolean shuffling;
    // new
    public volatile boolean freezeAll;
    public volatile boolean[][] dealerToPlayer; // new

    public LinkedBlockingQueue<Integer> sets;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        dealerToPlayer = new boolean[2][players.length];
        this.sets = new LinkedBlockingQueue<>();
        shuffling = new AtomicBoolean(true);
        this.freezeAll = true;

    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        for (int i = 0; i < env.config.players; i++) {
            Thread t = new Thread(players[i]);
            t.start();
        }
        while (!shouldFinish()) {
            synchronized (table) {
                this.freezeAll = true;
                removeAllCardsFromTable();
                placeCardsOnTable();
               
                updateTimerDisplay(true);
            }
            timerLoop();
        }
        announceWinners();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");

    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did
     * not time out.
     */

    private void timerLoop() {

        this.freezeAll = false;
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
            if(env.util.findSets(deck, 1).size() == 0)
            terminate();
        
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        // make terminate for all threads
        terminate = true;
        for (int i = players.length - 1; i >= 0; i--) {
            players[i].terminate();
        }
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     * 
     * @throws InterruptedException
     */
    private void removeCardsFromTable() {
        // TODO implement
        Integer id = sets.poll();
        if (id != null) {
            if(players[id].pset.size() != env.config.featureSize){
                try {
                    players[id].dealerResponse.put(-1);
                } catch (InterruptedException e) {}
                return;
            }
            LinkedBlockingQueue<Integer> lst = players[id].pset; // selected slots
            List<Integer> lst2 = new LinkedList<>(lst);// copy
            int[] set = new int[env.config.featureSize];
            int[] setCard = new int[env.config.featureSize];
            int i = 0;
            for (Integer slot: lst) {
                if (table.slotToCard[slot] != null) {
                    setCard[i] = table.slotToCard[slot]; //
                    set[i] = slot;
                    i++;
                }
            }
            boolean legall = env.util.testSet(setCard);
            if (legall) {
                synchronized (table.locks[set[0]]) {
                    synchronized (table.locks[set[1]]) {
                        synchronized (table.locks[set[2]]) {
                            // delete for id
                            sets.remove(id);
                            // check if a player in sets submitted a set with one of the cards, answer him
                            // with null
                            // remove any token from the table + from the players' pset
                            for (Player p : players) {
                                if (p.getId() != id) {
                                    boolean removed = p.removeItem(lst2.get(0)) || p.removeItem(lst2.get(1))
                                            || p.removeItem(lst2.get(2));
                                    if (removed) {
                                        try {
                                            players[p.getId()].dealerResponse.put(-1);
                                        } catch (InterruptedException e) {  }
                                        sets.remove((Integer)p.getId());
                                    }

                                }
                            }
                            for(i = 0; i < set.length; i++){
                                table.removeCard(set[i]);
                            }
                        }
                    }
                }
                updateTimerDisplay(true);
            }
            try {
                if(legall)
                players[id].dealerResponse.put(1);
                else players[id].dealerResponse.put(0);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement

        Collections.shuffle(this.deck);
        for (int i = 0; i < table.slotToCard.length; i++) {

            if ((!deck.isEmpty()) && table.slotToCard[i] == null)

                table.placeCard(deck.remove(0), i);
        }

        
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some
     * purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement

    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement

        if (reset) {
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
            env.ui.setCountdown(env.config.turnTimeoutMillis, true);
        } else // update??
        {

            boolean isRed = (reshuffleTime - System.currentTimeMillis()) < env.config.turnTimeoutWarningMillis;
            env.ui.setCountdown(reshuffleTime - System.currentTimeMillis(), isRed);

        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement

        // add to deck
        for (int i = 0; i < table.slotToCard.length; i++) {
            if (table.slotToCard[i] != null) {
                int card = table.slotToCard[i];
                deck.add(card);
            }
        }

        table.removeall();

        // delete player pest\
        for(Integer id: sets){
            try {
                players[id].dealerResponse.put(-1);
            } catch (InterruptedException e) {}
        }
        for (Player p : players) {
            p.pset.clear();
        }
        sets.clear();

        // intil
        env.ui.removeTokens();

    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement

        int maxScore = 0;
        int size = 0;
        List<Integer> winners = new ArrayList<>();
        for (Player p : players) {
            if (p.score() > maxScore) {
                maxScore = p.score();
                winners.clear();
                winners.add(p.getId());
                size = 1;  
            } else if (p.score() == maxScore) {
                winners.add(p.getId());
                size++;
            }
        }

        // list to array
        int[] win = new int[size];
        int i = 0;
        for (Integer id : winners) {
            win[i] = id;
            i++;
        }
        env.ui.announceWinner(win);
    }
}