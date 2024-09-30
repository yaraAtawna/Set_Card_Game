package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.LinkedList;
import java.util.List;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate
     * key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;
    public LinkedBlockingQueue<Integer> pset;
    LinkedBlockingQueue<Integer> pressedKey;
    LinkedBlockingQueue<Integer> dealerResponse;
    public volatile boolean isFreez;
    private Dealer dealer;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided
     *               manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        // add new
        this.dealer = dealer;
        this.pset = new LinkedBlockingQueue<Integer>();
        this.pressedKey = new LinkedBlockingQueue<>();
        dealerResponse = new LinkedBlockingQueue<>();
        this.isFreez = false;
    }

    /**
     * The main player thread of each player starts here (main loop for the player
     * thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human)
            createArtificialIntelligence();

        while (!terminate) {
            // TODO implement main player loop

            if (dealer.shuffling.get()) {
                try {
                    wait();
                } catch (Exception e) {
                }
            }
            Integer slot;
            try {
                slot = pressedKey.take();
                //System.out.println("Took out slot: " + slot);
                if (pset.contains(slot)) { // remove token from the slot
                    table.removeToken(id, slot);
                    pset.remove(slot);
                    // pest remove token !!
                } else if (pset.size() < env.config.featureSize) { // place token
                    table.placeToken(id, slot);
                    pset.add(slot);
                    // made a set?
                    if (pset.size() == env.config.featureSize) {
                        dealer.sets.put(id);
                        Integer ans = dealerResponse.take();
                        //System.out.print(ans);
                        if (ans != null) {
                            if (ans == 1) {
                                point();
                                // remove tokens and cards
                                for (Integer s : pset) {
                                    table.removeToken(id, s);
                                }
                                pset.clear();

                            } else if(ans == 0) {
                                penalty();
                            }
                        } else {
                            // Check if we need to do something here
                        }
                    }
                }
            } catch (InterruptedException e) {
            }
        }

        if (!human)
            try {
                aiThread.join();
            } catch (InterruptedException ignored) {
            }
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of
     * this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it
     * is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
              
                int key = (int) (Math.random() * env.config.tableSize);
                keyPressed(key);
                try{
                    Thread.sleep(2);    //sleep a little bit
                } catch(InterruptedException e){}

            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
        playerThread.interrupt();
        try{
            
            playerThread.join();
           
        } catch(InterruptedException e){}
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
        try {
            if (this.isFreez == false && dealer.freezeAll == false) {
                pressedKey.put(slot);
                //System.out.println("Pressed " + slot);
            } else {
                //System.out.println("in freeze time cant Pressed ");
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        this.isFreez = true;
        score++;
        env.ui.setScore(id, score);
        // Freeze the player for a second
        long freezTime = env.config.pointFreezeMillis;
        long sleepTime = 1000;
        while (freezTime > 0) {
            env.ui.setFreeze(id, freezTime);
            if (freezTime < 1000) {
                sleepTime = freezTime;
            }
            freezTime -= 1000;
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ignored) {
            }
        }
        freezTime = 0;
        env.ui.setFreeze(id, freezTime);
        this.isFreez = false;

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        this.isFreez = true;
        long penaltyTime = env.config.penaltyFreezeMillis;
        long sleepTime = 1000;
        while (penaltyTime > 0) {
            env.ui.setFreeze(id, penaltyTime);
            if (penaltyTime < 1000) {
                sleepTime = penaltyTime;
            }
            penaltyTime -= 1000;
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ignored) {
            }
        }
        penaltyTime = 0;
        env.ui.setFreeze(id, penaltyTime);
        this.isFreez = false;
    }

    public int score() {
        return score;
    }

    // new
    public int getId() {
        return id;
    }

    public boolean removeItem(Integer integer) {
        if (pset.contains(integer)) {
            env.ui.removeToken(id, integer);
            pset.remove(integer);
            return true;
        }
        return false;
    }
}
