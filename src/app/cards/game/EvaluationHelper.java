package app.cards.game;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import app.cards.Card;
import app.cards.Rank;
import app.cards.RankComparator;
import app.cards.Suit;

class EvaluationHelper {

	static CardSequence getLongestSequence(Set<Card> cards) {
		Set<CardSequence> allSequences = new HashSet<>();
		CardSequence currentSequence = new CardSequence();
		List<Card> sortedCards = new ArrayList<>(cards);
		Collections.sort(sortedCards, new RankComparator());
		
		// build up all sequences
		for (Card card : sortedCards) {
			if (card.getRank() != Rank.ACE && currentSequence.isEmpty()) {
				currentSequence.appendCard(card);
			} else if (card.getRank() != Rank.ACE && card.getRank().getValue() ==
					currentSequence.getRankOfLastElement().getValue() + 1) {
				// current card is successor of last in sequence
				currentSequence.appendCard(card);
			} else if (!currentSequence.isEmpty() &&
					card.getRank().getValue() ==
					currentSequence.getRankOfLastElement().getValue()) {
				// special case for multiple possible straights
				// (important because one of them might be a straight flush)
				currentSequence.addCardToLastElement(card);
			} else if (card.getRank() == Rank.ACE) {
				// special case for wheel straights
				// we need to re-check all sequences so far
				for (CardSequence oldSequence : allSequences) {
					if (oldSequence.getRankOfFirstElement() == Rank.DEUCE) {
						// put the ace at the beginning of the possible wheel
						oldSequence.prependCard(card);
					} else if (oldSequence.getRankOfLastElement() == Rank.KING) {
						// put the ace at the end of a possible broadway
						oldSequence.appendCard(card);
					} else if (oldSequence.getRankOfFirstElement() == Rank.ACE) {
						// there is already an ace at the beginning, add this one as well
						// to offer more choice in case of a straight flush
						oldSequence.addCardToFirstElement(card);
					} else if (oldSequence.getRankOfLastElement() == Rank.ACE) {
						// there is already an ace at the end, add this one as well
						// to offer more choice in case of a straight flush
						oldSequence.addCardToLastElement(card);
					}
				}
			} else {
				// all cases fell through, sequence has ended
				allSequences.add(currentSequence);
				
				// start new
				currentSequence = new CardSequence();
				currentSequence.appendCard(card);
			}
		}
		
		// flush out the last current sequence
		allSequences.add(currentSequence);
		
		// find the longest sequence
		CardSequence longestSequence = null;
		
		for (CardSequence sequence : allSequences) {
			if (longestSequence == null) {
				// any sequence is better than no sequence
				longestSequence = sequence;
			} else if (sequence.length() > longestSequence.length()) {
				longestSequence = sequence;
			} else if (sequence.length() == longestSequence.length()) {
				// if there's a tie, pick the sequence with the higher card
				if (sequence.getRankOfLastElement().getValue() >
						longestSequence.getRankOfLastElement().getValue()) {
					longestSequence = sequence;
				}
			}
		}
		
		return longestSequence;
	}
	
	public static Map<Rank, Set<Card>> groupCardsByRank(Set<Card> cards) {
		Map<Rank, Set<Card>> groupedRanks = new HashMap<>();
		
		for (Card card : cards) {
			Rank rank = card.getRank();
			if (groupedRanks.containsKey(rank)) {
				groupedRanks.get(rank).add(card);
			} else {
				Set<Card> rankCards = new HashSet<>();
				rankCards.add(card);
				groupedRanks.put(rank, rankCards);
			}
		}
		
		return groupedRanks;
	}
	
	public static Map<Suit, Set<Card>> groupCardsBySuit(Set<Card> cards) {
		Map<Suit, Set<Card>> groupedSuits = new HashMap<>();
		
		for (Card card : cards) {
			Suit suit = card.getSuit();
			if (groupedSuits.containsKey(suit)) {
				groupedSuits.get(suit).add(card);
			} else {
				Set<Card> suitCards = new HashSet<>();
				suitCards.add(card);
				groupedSuits.put(suit, suitCards);
			}
		}
		
		return groupedSuits;
	}
	
	public static List<NOfAKind> getNOfAKinds(Set<Card> cards) {
		Map<Rank, Set<Card>> groupedRanks = groupCardsByRank(cards);
		List<NOfAKind> nOfAKinds = new ArrayList<>();
		
		for (Rank rank : groupedRanks.keySet()) {
			nOfAKinds.add(new NOfAKind(groupedRanks.get(rank)));
		}
		
		Collections.sort(nOfAKinds);
		
		return nOfAKinds;
	}
	
	public static void main(String[] args) {
		Set<Card> cards = new HashSet<>();
		cards.add(Card.getCard(Suit.CLUBS, Rank.FIVE));
		cards.add(Card.getCard(Suit.HEARTS, Rank.FIVE));
		cards.add(Card.getCard(Suit.CLUBS, Rank.TEN));
		cards.add(Card.getCard(Suit.DIAMONDS, Rank.FIVE));
		cards.add(Card.getCard(Suit.HEARTS, Rank.TEN));
		cards.add(Card.getCard(Suit.HEARTS, Rank.TRAY));
		cards.add(Card.getCard(Suit.SPADES, Rank.TEN));
		
		System.out.println(getNOfAKinds(cards));
	}
	
	@Test
	public void testStraightDetection() {
		Set<Card> cards = new HashSet<>();
		cards.add(Card.getCard(Suit.CLUBS, Rank.FIVE));
		cards.add(Card.getCard(Suit.HEARTS, Rank.FOUR));
		cards.add(Card.getCard(Suit.CLUBS, Rank.TEN));
		cards.add(Card.getCard(Suit.DIAMONDS, Rank.JACK));
		cards.add(Card.getCard(Suit.HEARTS, Rank.SIX));
		cards.add(Card.getCard(Suit.HEARTS, Rank.TRAY));
		cards.add(Card.getCard(Suit.SPADES, Rank.SEVEN));
		
		CardSequence result = EvaluationHelper.getLongestSequence(cards);
		
		assertEquals("Longest sequence should be 5 elements long", 5, result.length());
		assertEquals(Card.getCard(Suit.HEARTS, Rank.TRAY), result.anyCardAt(0));
		assertEquals(Card.getCard(Suit.HEARTS, Rank.FOUR), result.anyCardAt(1));
		assertEquals(Card.getCard(Suit.CLUBS, Rank.FIVE), result.anyCardAt(2));
		assertEquals(Card.getCard(Suit.HEARTS, Rank.SIX), result.anyCardAt(3));
		assertEquals(Card.getCard(Suit.SPADES, Rank.SEVEN), result.anyCardAt(4));
	}
	
	@Test
	public void testWheelDetection() {
		Set<Card> cards = new HashSet<>();
		cards.add(Card.getCard(Suit.CLUBS, Rank.FIVE));
		cards.add(Card.getCard(Suit.HEARTS, Rank.TRAY));
		cards.add(Card.getCard(Suit.CLUBS, Rank.ACE));
		cards.add(Card.getCard(Suit.DIAMONDS, Rank.KING));
		cards.add(Card.getCard(Suit.HEARTS, Rank.ACE));
		cards.add(Card.getCard(Suit.HEARTS, Rank.FOUR));
		cards.add(Card.getCard(Suit.SPADES, Rank.DEUCE));
		
		CardSequence result = EvaluationHelper.getLongestSequence(cards);
		
		assertEquals("Longest sequence should be 5 elements long", 5, result.length());
		assertTrue(result.cardsAt(0).contains(Card.getCard(Suit.HEARTS, Rank.ACE)));
		assertTrue(result.cardsAt(0).contains(Card.getCard(Suit.CLUBS, Rank.ACE)));
		assertEquals(Card.getCard(Suit.SPADES, Rank.DEUCE), result.anyCardAt(1));
		assertEquals(Card.getCard(Suit.HEARTS, Rank.TRAY), result.anyCardAt(2));
		assertEquals(Card.getCard(Suit.HEARTS, Rank.FOUR), result.anyCardAt(3));
		assertEquals(Card.getCard(Suit.CLUBS, Rank.FIVE), result.anyCardAt(4));
	}
	
	@Test
	public void testConcurrentSequenceDetection() {
		Set<Card> cards = new HashSet<>();
		cards.add(Card.getCard(Suit.CLUBS, Rank.QUEEN));
		cards.add(Card.getCard(Suit.HEARTS, Rank.JACK));
		cards.add(Card.getCard(Suit.CLUBS, Rank.EIGHT));
		cards.add(Card.getCard(Suit.DIAMONDS, Rank.KING));
		cards.add(Card.getCard(Suit.HEARTS, Rank.SIX));
		cards.add(Card.getCard(Suit.HEARTS, Rank.DEUCE));
		cards.add(Card.getCard(Suit.SPADES, Rank.SEVEN));
		
		CardSequence result = EvaluationHelper.getLongestSequence(cards);
		
		assertEquals("Longest sequence should be 3 elements long", 3, result.length());
		for (Set<Card> cardSet : result) {
			assertEquals(1, cardSet.size());
		}
		assertEquals(Card.getCard(Suit.HEARTS, Rank.JACK), result.anyCardAt(0));
		assertEquals(Card.getCard(Suit.CLUBS, Rank.QUEEN), result.anyCardAt(1));
		assertEquals(Card.getCard(Suit.DIAMONDS, Rank.KING), result.anyCardAt(2));
	}
	
	@Test
	public void testMultipleCardStraightDetection() {
		Set<Card> cards = new HashSet<>();
		cards.add(Card.getCard(Suit.CLUBS, Rank.FIVE));
		cards.add(Card.getCard(Suit.HEARTS, Rank.EIGHT));
		cards.add(Card.getCard(Suit.CLUBS, Rank.NINE));
		cards.add(Card.getCard(Suit.DIAMONDS, Rank.SIX));
		cards.add(Card.getCard(Suit.HEARTS, Rank.SIX));
		cards.add(Card.getCard(Suit.HEARTS, Rank.ACE));
		cards.add(Card.getCard(Suit.SPADES, Rank.SEVEN));
		
		CardSequence result = EvaluationHelper.getLongestSequence(cards);
		System.out.println(result);
		
		assertEquals("Longest sequence should be 5 elements long", 5, result.length());
		assertEquals(1, result.cardsAt(0).size());
		assertEquals(Card.getCard(Suit.CLUBS, Rank.FIVE), result.anyCardAt(0));
		assertEquals(2, result.cardsAt(1).size());
		assertTrue(result.cardsAt(1).contains(Card.getCard(Suit.DIAMONDS, Rank.SIX)));
		assertTrue(result.cardsAt(1).contains(Card.getCard(Suit.HEARTS, Rank.SIX)));
		assertEquals(1, result.cardsAt(0).size());
		assertEquals(Card.getCard(Suit.SPADES, Rank.SEVEN), result.anyCardAt(2));
		assertEquals(1, result.cardsAt(0).size());
		assertEquals(Card.getCard(Suit.HEARTS, Rank.EIGHT), result.anyCardAt(3));
		assertEquals(1, result.cardsAt(0).size());
		assertEquals(Card.getCard(Suit.CLUBS, Rank.NINE), result.anyCardAt(4));
	}
	
}
