package voteCounter;

import java.util.ArrayList;

/**
 * Represents an individual ballot
 */
public class Vote {
	
	//the name of the voter
	private String voter;
	
	//The votes list of choices, in order, with index 0 being the first choice
	private ArrayList<String> choices = new ArrayList<>();
	//The index of choices that this vote is currently being counted for
	private int currentChoice;
	private double weight = 1.0;
	
	public Vote(String[] inputRow) {
		voter = inputRow[0].trim();
		
		for(int i=1; i<inputRow.length; i++) {
			choices.add(inputRow[i].trim());
		}
		
		currentChoice = 0;
	}
	
	/**
	 * Getter for voter
	 * 
	 * @return String voter
	 */
	public String getVoter() {
		return this.voter;
	}
	
	/**
	 * Getter for choices
	 * 
	 * @return ArrayList<String> choices
	 */
	public ArrayList<String> getChoices() {
		return this.choices;
	}
	
	/**
	 * Retrieves the current choice of the vote from choices, using currentChoice as the index
	 * 
	 * @return String the current choice
	 */
	public String getChoice() {
		return choices.get(currentChoice);
	}
	
	/**
	 * Removes a candidate from the list of eligible candidates on the ballot
	 * 
	 * @param candidate - the candidate to eliminate from this ballot
	 */
	public void eliminateRanking(String candidate) {
		if(choices != null && !choices.isEmpty()) {
			for(int i=0; i<choices.size(); i++) {
				if(choices.get(i).compareTo(candidate) == 0) {
					choices.remove(i);
					
					//If we removed a higher in the list than the current choice, we must move the current choice back
					//	so that is still matches the correct candidate
					if(i<currentChoice) {
						currentChoice--;
					}
					
					break;
				}
			}
		}
	}
	
	/**
	 * Remove the candidate only if they are the first choice of this vote
	 * 
	 * @param candidate
	 */
	public void eliminateFirstRanking(String candidate) {
		if(choices != null && !choices.isEmpty()) {
			if(choices.get(0).compareTo(candidate) == 0) {
				choices.remove(0);
			}
		}
	}
	
	/**
	 * Increments the current choice of this vote to the next candidate in the list
	 * @return boolean - True if the current choice was actually incremented, false otherwise.
	 */
	public boolean incrementCurrentChoice() {
		if(currentChoice+1 < choices.size()) {
			this.currentChoice++;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Resets the current choice to the vote's first choice
	 */
	public void resetCurrentChoice() {
		this.currentChoice = 0;
	}
	
	/**
	 * Getter for current choice
	 * 
	 * @return int currentChoice
	 */
	public int getCurrentChoice() {
		return this.currentChoice;
	}
	
	public double getWeight() {
		return weight;
	}
	
	public void alterWeight(double value) {
		weight *= value;
	}
	
	/**
	 * Return the ranking position of a candidate on this vote
	 * 
	 * @param candidate - String - the candidate to check
	 * @return int - The position of that candidate in this vote's rankings
	 */
	public int getPoints(String candidate) {
		for(int i=0; i<choices.size(); i++) {
			if(choices.get(i).compareTo(candidate) == 0) {
				return (choices.size() - i);
			}
		}
		
		//return 0 if they were not found on this ballot
		return 0;
	}
	
	/**
	 * Generate a matrix that marks if a candidate beats a every other candidate on the ballot based on their rankings.
	 * the X-Axis represents the runner and the Y-axis represents the opponent. A 1 in the respective spot means
	 * that the runner defeats the opponent. 
	 * 
	 * @param candidates - This list of candidates to create the matrix for
	 * @return int[][] - A head-to-head victory matrix
	 */
	public int[][] generateMatrix(ArrayList<String> candidates) {
		int[][] matrix = new int[candidates.size()][candidates.size()];
		
		for(int x=0; x<candidates.size(); x++) {
			//get the runner
			String runner = candidates.get(x);
			
			for(int y=0; y<candidates.size(); y++) {
				//get the opponent
				String opponent = candidates.get(y);
				
				//make sure they aren't the same candidate
				if(runner.compareTo(opponent) != 0) {
					
					//get the index of both candidates in this votes ranking array
					int runnerIndex = -1;
					int opponentIndex = -1;
					
					for(int i=0; i<choices.size(); i++) {
						if(choices.get(i).compareTo(runner) == 0) {
							runnerIndex = i;
						} else if(choices.get(i).compareTo(opponent) == 0) {
							opponentIndex = i;
						}
					}
					
					//If the runner was found and the opponent was not, the runner wins
					// else if the runner was found and their index is closer to 0 than the opponent,
					// the runner wins. All other situations result in the default 0 value being kept.
					if(runnerIndex != -1 && opponentIndex == -1) {
						matrix[x][y] = 1;
					} else if(runnerIndex != -1 && runnerIndex < opponentIndex) {
						matrix[x][y] = 1;
					}

				}
			}
		}
		
		return matrix;
	}
}
