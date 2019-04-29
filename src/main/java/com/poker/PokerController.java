package com.poker;

import java.util.ArrayList;
import java.util.Scanner;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderErrors;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;

/**
 * This is a sample class to launch a rule.
 */
public class PokerController {

    public static final void main(String[] args) {
        try {
            KnowledgeBase kbase = readKnowledgeBase();
            StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();
            
            Game message = new Game();
            
            ksession.insert(message);
            ksession.fireAllRules();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static KnowledgeBase readKnowledgeBase() throws Exception {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newClassPathResource("AgentHoldEm.drl"), ResourceType.DRL);
        KnowledgeBuilderErrors errors = kbuilder.getErrors();
        if (errors.size() > 0) {
            for (KnowledgeBuilderError error: errors) {
                System.err.println(error);
            }
            throw new IllegalArgumentException("Could not parse knowledge.");
        }
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        return kbase;
    }

    public static class Game {
    	public static final int PLAYER = 0;
    	public static final int AGENT = 1;
    	
    	public static final int NEW_GAME = 0;
    	public static final int NEW_HAND = 1;
    	public static final int PREFLOP = 2;
    	public static final int FLOPDEAL = 3;
    	public static final int FLOP = 4;
    	public static final int TURNDEAL = 5;
    	public static final int TURN = 6;
    	public static final int RIVERDEAL = 7;
    	public static final int RIVER = 8;
    	public static final int SHOWDOWN = 9;
    	public static final int END = 10;
    	
    	public boolean dataUpdated = false;
    	
    	public static final int SMALL_BLIND = 25;
    	public static final int BIG_BLIND = 50;
    	public static final int CHIPS = 1000;
    	
    	private int gameState; 
        private int smallBlind;
    	private int bigBlind;
    	private int pot;  	
    	
        private ArrayList<Card> deck;
    	private ArrayList<Card> agentHand;
    	private ArrayList<Card> playerHand;
    	private ArrayList<Card> board;

    	
    	private boolean playerAllIn;
    	private boolean agentAllIn;
    	private int playerChipsStartOfHand;
    	private int agentChipsStartOfHand;
		private int playerChips;
    	private int agentChips;
    	
    	private int currentBet;
    	private boolean prevBetOrCheck;
    	private int dealer;
    	private int turn;
    		
		private char[] suits = {(char)'\u2665', (char)'\u2666', (char)'\u2663', (char)'\u2660'}; 
    	private String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
    	
    	private Scanner scnr = new Scanner(System.in);
		private boolean agentEvaluation;
		private double currentHS;
		private double currentEHS;
		private double currentPPot;
		private double currentNPot;
    	
    	public Game() {
    		this.turn = 0;
    		this.dealer = 0;
    		if(Math.random() < 0.5) {
    			this.turn = 1;
    			this.dealer = 1;
    		}
    	}
    	
    	public void newGame() {
    		this.gameState = Game.NEW_GAME;
        	this.playerChips = Game.CHIPS;
        	this.agentChips = Game.CHIPS;
        	this.smallBlind = Game.SMALL_BLIND;
        	this.bigBlind = Game.BIG_BLIND;
        	this.pot = 0;
        	this.agentEvaluation = false;
        	this.currentHS = 0;
        	this.currentEHS = 0;
        	this.currentPPot = 0;
        	this.currentNPot = 0;
    	}

		public void newHand() {
        	agentHand = new ArrayList<Card>();
        	playerHand = new ArrayList<Card>();
        	board = new ArrayList<Card>();
    		deck = buildDeck();
    		dealHandCards();
    		this.currentBet = 0;
        	this.pot = 0;
    		dealer = dealer == PLAYER ? AGENT : PLAYER;
    		turn = dealer;
    		dataUpdated = false;
    		agentChipsStartOfHand = agentChips;
    		playerChipsStartOfHand = playerChips;
    		setAgentAllIn(false);
    		setPlayerAllIn(false);
    		postBlinds();
    	}
    	
    	public static ArrayList<Card> buildDeck() {
    		ArrayList<Card> temp = new ArrayList<Card>();
    		for(int i = 0; i < 4; i++) {
    			for(int j = 0; j < 13; j++) {
    				temp.add(new Card(i, j));
    			}
    		}
    		return temp;
    	}
    	
    	public void dealBoardCard() {
    		board.add(deck.remove((int)(Math.random() * 1000000) % deck.size()));
    	}
    	
    	public void printBoardCards() {
    		System.out.println("Board Cards:");
    		for(Card c: board) {
    			System.out.print(ranks[c.getRank()] + suits[c.getSuit()] + " ");
    		}
    		System.out.println("");
    	}
    	

    	public void dealHandCards() {
    		playerHand.add(deck.remove((int)(Math.random() * 1000000) % deck.size()));
    		playerHand.add(deck.remove((int)(Math.random() * 1000000) % deck.size()));
    		
    		agentHand.add(deck.remove((int)(Math.random() * 1000000) % deck.size()));
    		agentHand.add(deck.remove((int)(Math.random() * 1000000) % deck.size()));
    		
    	}
    	
    	public void printPlayerHand() {
    		System.out.println("Your Cards:");
    		for(Card c: playerHand) {
    			System.out.print(ranks[c.getRank()] + suits[c.getSuit()] + " ");
    		}
    		System.out.println("");
    	}
    	
    	public void printAgentHand() {
    		System.out.println("Agent Cards:");
    		for(Card c: agentHand) {
    			System.out.print(ranks[c.getRank()] + suits[c.getSuit()] + " ");
    			System.out.println("");
    		}
    	}
    	
    	public static int indexOfCard(ArrayList<Card> cards, Card card) {
    		int count = 0;
    		for(Card comp : cards) {
    			if (card.equals(comp)) {
    				return count;
    			}
    			count++;
    		}
    		
    		return -1;
    	}
    	
    	public void playerBet(int chipsToCall) {
    		if(playerAllIn == true) {
    			return;
    		}
    		printPlayerHand();
    		System.out.println("");
    		System.out.println("Enter your choice:");
    		if(chipsToCall > 0) {
	    		System.out.println("1. Call " + chipsToCall + " chips");
	    		System.out.println("2. Raise");
	    		System.out.println("3. Fold");
	    		bettingRules(scnr.nextInt(), chipsToCall);
    		}
    		else {
    			System.out.println("1. Check");
	    		System.out.println("2. Bet - Must be greater than " + bigBlind + " chips");
	    		System.out.println("3. Fold");
	    		bettingRules(scnr.nextInt(), chipsToCall);
    		}
    	}
    	
    	public void agentBet(int chipsToBet, int chipsToCall) {
    		if(agentAllIn == true) {
    			return;
    		}
    		if(chipsToBet + chipsToCall >= agentChips) {
    			agentGoAllIn();
    			System.out.println("Agent went All-In");
    		} 
    		else {
    			System.out.println("Agent raises by " + chipsToBet + " chips");
        		pot += chipsToBet + chipsToCall;
    			agentChips -= chipsToBet + chipsToCall;
    			
    			currentBet = chipsToBet;
    			prevBetOrCheck = true;
    			turn = PLAYER;
    		}
			return;
    	}
    	
    	public void agentCall(int chipsToCall) {
    		if(chipsToCall >= agentChips) {
    			agentGoAllIn();
    			System.out.println("Agent went All-In");
    		} 
			else {
				if(chipsToCall !=0) {
					System.out.println("Agent calls " + chipsToCall + " chips");
				}	
				else {
					System.out.println("Agent checks");
				}
    			pot += chipsToCall;
    			agentChips -= chipsToCall;
    			currentBet = 0;
    			turn = PLAYER;
   
    			if(isPrevBetOrCheck()) {
    				gameState += 1;
    				turn = dealer == PLAYER ? AGENT : PLAYER;
    				prevBetOrCheck = false;
    			}
    			else {
    				setPrevBetOrCheck(true);
    			}
			}
    	}
    	
    	public void agentFold() {
    		System.out.println("Agent has folded");
    		playerWin();
    		setAgentEvaluation(false);
    		gameState = NEW_HAND;
  
    	}
    	
    	public void bettingRules(int choice, int chipsToCall) {
    		int bet = 0;
    		
    		if(choice == 1) {
    			if(chipsToCall >= playerChips) {
        			playerGoAllIn();
        		} 
    			else {
	    			pot += chipsToCall;
	    			playerChips -= chipsToCall;
	    			currentBet = 0;
	    			turn = AGENT;
	   
	    			if(isPrevBetOrCheck()) {
	    				gameState += 1;
	    				turn = dealer == PLAYER ? AGENT : PLAYER;
	    				prevBetOrCheck = false;
	    			}
	    			else {
	    				setPrevBetOrCheck(true);
	    			}
    			}
    			return;
    		}
    		if(choice == 2) {
    			System.out.println("Enter the amount you want to raise by: ");
    			bet = scnr.nextInt();
    			
    			if(chipsToCall > bigBlind) {
    				while(bet < chipsToCall) {
        				System.out.println("Raise must be at least" + chipsToCall + " chips");
        				bet = scnr.nextInt();
        			}
    			}
    			else {
    				while(bet < bigBlind) {
        				System.out.println("Raise must be at least" + bigBlind + " chips");
        				bet = scnr.nextInt();
        			}
    			}
    			currentBet = bet;
    			if((bet + chipsToCall) >= playerChips) {
    				playerGoAllIn();
    			}
    			else {
	    			pot += bet + chipsToCall;
	    			playerChips -= bet + chipsToCall;
	    			
    			}
    			setPrevBetOrCheck(true);
    			
    			turn = AGENT;
    		}
    		else {
    			agentChips += pot;
    			pot = 0;
    			gameState = NEW_HAND;
    			return;
    		}
    	}
    	
    	public void playerGoAllIn() {
    		pot += playerChips;
    		playerChips = 0;
    		setPlayerAllIn(true);
    		if(pot > playerChipsStartOfHand * 2) {
    			agentChips += (pot - (playerChipsStartOfHand * 2));
    			pot = playerChipsStartOfHand * 2;
    		}
    		else {
    			currentBet = (playerChipsStartOfHand * 2) - pot;
    			if(currentEHS > 0.90) {
    				agentBet(0, currentBet);
    			}
    			else if(currentEHS == 0 && currentHS > 0.90) {
    				agentBet(0, currentBet);
    			}
    			else {
    				agentFold();
    			}
    		}
    	}
    	
    	public void agentGoAllIn() {
    		pot += agentChips;
    		agentChips = 0;
    		setAgentAllIn(true);
    		if(pot > agentChipsStartOfHand * 2) {
    			playerChips += (pot - (agentChipsStartOfHand * 2));
    			pot = agentChipsStartOfHand * 2;
    		}
    		else {
    			currentBet = (agentChipsStartOfHand * 2) - pot;
    			playerBet(currentBet);
    		}
    	}
    	
    	
    	public void postBlinds() {
    		if (dealer == PLAYER) {
    			if (playerChips < smallBlind) {
    				pot += playerChips;
    				playerChips = 0;
    			}
    			else
    			{
    				pot += smallBlind;
    				playerChips -= smallBlind;
    			}
    			if (agentChips < bigBlind) {
    				pot += agentChips;
    				agentChips = 0;
    			}
    			else
    			{
    				pot += bigBlind;
    				agentChips -= bigBlind;
    			}
    		} 
    		else {
    			if (playerChips < bigBlind) {
    				pot += playerChips;
    				playerChips = 0;
    			}
    			else
    			{
    				pot += bigBlind;
    				playerChips -= bigBlind;
    			}
    			if (agentChips < smallBlind) {
    				pot += agentChips;
    				agentChips = 0;
    			}
    			else
    			{
    				pot += smallBlind;
    				agentChips -= smallBlind;
    			}
    		}
    	}
    	
    	public void printHand(ArrayList<Card> hand) {
    		for(Card c: hand) {
    			System.out.print(ranks[c.getRank()] + suits[c.getSuit()] + " ");
    		}
    		System.out.println("");
    	}
    	
    	public void agentWin() {
    		agentChips += pot;
    		pot = 0;
    	}
    	
    	public void playerWin() {
    		playerChips += pot;
    		pot = 0;
    	}

    	public void splitPot() {
    		playerChips += pot/2;
    		agentChips += pot/2;
    		pot = 0;
    	}
    	
    	public void updateData() {
    		this.currentHS = Evaluator.handStrength(this.agentHand, this.board);
    		if (this.gameState > Game.PREFLOP) {
        		double[] temp = Evaluator.handPotential(this.agentHand, this.board);
        		this.currentPPot = temp[1];
        		this.currentNPot = temp[2];
        		this.currentEHS = Evaluator.effectiveHandStrength(this.currentEHS, this.currentPPot);
        		dataUpdated = true;
    		}
    	}
    	
		public int getGameState() {
			return gameState;
		}

		public void setGameState(int gameState) {
			this.gameState = gameState;
		}

		public ArrayList<Card> getAgentHand() {
			return agentHand;
		}

		public void setAgentHand(ArrayList<Card> agentHand) {
			this.agentHand = agentHand;
		}

		public ArrayList<Card> getPlayerHand() {
			return playerHand;
		}

		public void setPlayerHand(ArrayList<Card> playerHand) {
			this.playerHand = playerHand;
		}

		public ArrayList<Card> getBoard() {
			return board;
		}

		public void setBoard(ArrayList<Card> board) {
			this.board = board;
		}

		public ArrayList<Card> getDeck() {
			return deck;
		}

		public void setDeck(ArrayList<Card> deck) {
			this.deck = deck;
		}

		public int getPlayerChips() {
			return playerChips;
		}

		public void setPlayerChips(int playerChips) {
			this.playerChips = playerChips;
		}

		public int getAgentChips() {
			return agentChips;
		}

		public void setAgentChips(int agentChips) {
			this.agentChips = agentChips;
		}

		public int getPot() {
			return pot;
		}

		public void setPot(int pot) {
			this.pot = pot;
		}

		public int getCurrentBet() {
			return currentBet;
		}

		public void setCurrentBet(int currentBet) {
			this.currentBet = currentBet;
		}

		public int getTurn() {
			return turn;
		}

		public void setTurn(int turn) {
			this.turn = turn;
		}

		public boolean isPrevBetOrCheck() {
			return prevBetOrCheck;
		}

		public void setPrevBetOrCheck(boolean prevBetOrCheck) {
			this.prevBetOrCheck = prevBetOrCheck;
		}
		
    	public int getSmallBlind() {
			return smallBlind;
		}

		public void setSmallBlind(int smallBlind) {
			this.smallBlind = smallBlind;
		}

		public int getBigBlind() {
			return bigBlind;
		}

		public void setBigBlind(int bigBlind) {
			this.bigBlind = bigBlind;
		}
		
    	public int getDealer() {
			return dealer;
		}

		public void setDealer(int dealer) {
			this.dealer = dealer;
		}

		public boolean getPlayerAllIn() {
			return playerAllIn;
		}

		public void setPlayerAllIn(boolean playerAllIn) {
			this.playerAllIn = playerAllIn;
		}

		public boolean getAgentAllIn() {
			return agentAllIn;
		}

		public void setAgentAllIn(boolean agentAllIn) {
			this.agentAllIn = agentAllIn;
		}
		
		public int getBoardSize() {
			return board.size();
		}
		
    	public boolean isAgentEvaluation() {
			return agentEvaluation;
		}

		public void setAgentEvaluation(boolean agentEvaluation) {
			if(agentEvaluation == true) {
				if(!dataUpdated) {
					this.updateData();
				}
			}
			this.agentEvaluation = agentEvaluation;
		}
		
		public double getCurrentHS() {
			return currentHS;
		}

		public void setCurrentHS(double currentHS) {
			this.currentHS = currentHS;
		}

		public double getCurrentEHS() {
			return currentEHS;
		}

		public void setCurrentEHS(double currentEHS) {
			this.currentEHS = currentEHS;
		}

		public double getCurrentPPot() {
			return currentPPot;
		}

		public void setCurrentPPot(double currentPPot) {
			this.currentPPot = currentPPot;
		}

		public double getCurrentNPot() {
			return currentNPot;
		}

		public void setCurrentNPot(double currentNPot) {
			this.currentNPot = currentNPot;
		}

		public boolean isDataUpdated() {
			return dataUpdated;
		}

		public void setDataUpdated(boolean dataUpdated) {
			this.dataUpdated = dataUpdated;
		}
    }
}
