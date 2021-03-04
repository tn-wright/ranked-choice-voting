package voteCounter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The main vote counting class. It reads the input arguments and files, analyzes the votes in rounds, 
 * and output the results of each round to the console<br>
 * <br>
 * Arguments:<br>
 * 	candidateFileName - String - The name of the file listing each candidate. Each candidate should be on a new line<br>
 * 	voteFileName - String - The name of the csv file containing all of the votes. The csv file should have a head and use a comma as the delimiter.<br>
 * 	numberOfWinners - int - The number of candidates that can win,
 */
public class VoteCounter {
	//Parameter: The number of candidates that can win
	static int numberOfWinners;
	//The percentage of votes that is needed to guarantee a win for a candidate
	static double voteThresholdPercent;
	//A counter for the total number of candidates
	static int numberOfCandidates = 0;
	//the total number of votes being counted
	static int numberOfVotes = 0;
	//the number of votes needed to guarantee a win
	static double voteThreshold = 0.0;
	//A list of the candidates which have won
	static ArrayList<String> winningCandidates = new ArrayList<>();
	//A list of the candidates which have been eliminated
	static ArrayList<String> eliminatedCandidates = new ArrayList<>();
	//A list of the original, unaltered Votes. Needed for the tie breaker
	static ArrayList<Vote> originalVotes = new ArrayList<Vote>();
	//A list of the votes that are redistributed
	static ArrayList<Vote> votes = new ArrayList<Vote>();
	//A list of the working vote counts
	static HashMap<String, Double> currentVoteCounts = new HashMap<>();
	static int roundCounter = 1;
	
	/**
	 * The main method. See class docs for description on required arguments
	 * 
	 * @param args - String[]
	 */
	public static void main(String[] args) {
		//If there was 1 argument, check if it was help and print how to if it was
		if(args.length == 1 && args[0].equals("help")) {
			//TODO provide how to output
			return;
		}
		
		//Quit if the three arguments were not supplied
		if(args.length != 3) {
			System.out.println("Please provide all 3 arguments. Run 'VoteCounter help' for more information.");
			return;
		}
		
		//Parse the input files
		if(!parseCandidates(args[0])) {
			//If parseCandidates returns false, then it failed to work correctly and the program should terminate
			return;
		}
		if(!parseVotes(args[1])) {
			//if parseVotes returns false, then it failed to work correctly and the program should terminate
			return;
		}
		
		//Try to get the number of voters
		try {
			numberOfWinners = Integer.parseInt(args[2]);
			
			if(numberOfWinners < 1 || numberOfWinners > currentVoteCounts.keySet().size()) {
				System.out.println("The number of winners must be at least 1 and no more than the number of candidates");
				return;
			}
		} catch (NumberFormatException e) {
			//Not a valid number, so print error to the user and return
			System.out.println("Invalid winner count argument. ");
			return;
		}
		
		//calculate the vote thresholds
		voteThresholdPercent = 1.0/(numberOfWinners+1);
		voteThreshold = numberOfVotes*voteThresholdPercent;
		
		//perform the initial count of the votes
		countVotes();
		
		//Analyze the votes, redistributing as needed, until enough winners have been found
		do {
			analyzeVotes();
		} while(winningCandidates.size() < numberOfWinners);
		
		//Print the final winners
		System.out.println();
		System.out.println("Winners:");
		System.out.println(String.join(", ", winningCandidates));
	}
	
	/**
	 * Parse the candidate file and initialize {@code numberOfCandidates} and {@code currentVoteCounts}
	 * 
	 * @param candidateFileName - String - the name of the candidate file
	 * @return boolean - true if successful, false if not successful
	 */
	private static boolean parseCandidates(String candidateFileName) {
		BufferedReader candidateReader = null;
		
		try {
			//try to open the file for reading
			candidateReader = new BufferedReader(new FileReader(candidateFileName));
			String candidate = null;
			
			//Loop through each line and store the candidate and increase the candidate count
			while((candidate=candidateReader.readLine()) != null) {
				currentVoteCounts.put(candidate.trim(), 0.0);
				numberOfCandidates += 1;
			}
		} catch(FileNotFoundException e) {
			//The file was not found, so print the error message and quit
			System.out.println("Unable to open the candidate file. Please ensure the provided file path is correct.");
			return false;
		} catch(IOException e) {
			//Something went wrong reading the file, so print the error message and quit
			System.out.println("An error occured while reading the candidate file:" + e.getMessage());
			return false;
		} finally {
			//Close the candidateReader after everything, if it exists
			if(candidateReader != null) {
				try {
					candidateReader.close();
				} catch (IOException e) {
					System.out.println("An error occured while closing the candidate file:\n" + e.getMessage());
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Parse the vote file and construct all {@link Vote} objects
	 * 
	 * @param voteFileName - String - The file name of the vote file
	 * @return boolean - true if successful, false if not successful
	 */
	private static boolean parseVotes(String voteFileName) {
		BufferedReader voteReader = null;
		
		try {
			//attempt to open the vote file
			voteReader = new BufferedReader(new FileReader(voteFileName));
			String line = null;
			voteReader.readLine();
			
			//parse the line of the csv file and use it to create a Vote object
			while((line = voteReader.readLine()) != null) {
				String[] inputRow = line.split(",");
				
				//store the vote as needed and count the total number of votes
				votes.add(new Vote(inputRow));
				originalVotes.add(new Vote(inputRow));
				numberOfVotes++;
			}
		} catch (FileNotFoundException e) {
			//File was not found, so return
			System.out.println("Unable to open the vote file. Please ensure the provided file path is correct.");
			return false;
		} catch (IOException e) {
			//Failure to read file correctly, so return
			System.out.println("An error occured while reading the vote file:" + e.getMessage());
			return false;
		} finally {
			//Close the buffered reader when finished
			if (voteReader != null) {
				try {
					voteReader.close();
				} catch (IOException e) {
					System.out.println("An error occured while closing the vote file:\n" + e.getMessage());
				}
			}
		}
		return true;
	}
	
	/**
	 * Analyzes the current vote counts, determining a winner or loser for the round, and the redistributes votes as needed
	 */
	private static void analyzeVotes() {
		//Print the start of round text and current vote counts
		System.out.println();
		System.out.println("Round " + roundCounter++ + " vote counts:");
		for(Map.Entry<String, Double> entry : currentVoteCounts.entrySet()) {
			System.out.print(entry.getKey() + ": " + entry.getValue() + " | ");
		}
		System.out.println();
		
		//If the number of candidates left equals the number of winners, mark them all winners and end
		if(currentVoteCounts.size() == numberOfWinners) {
			for(Map.Entry<String, Double> entry : currentVoteCounts.entrySet()) {
				if(!winningCandidates.contains(entry.getKey())) {
					winningCandidates.add(entry.getKey());
					System.out.println(entry.getKey() + " has won in the last round.");
				}
			}
			
			return;
		}
		
		boolean noWinnerFound = true;
		
		//create an arrayList that holds the factions with the least number of votes and track the current minimum vote total
		ArrayList<String> minCandidatess = new ArrayList<>();
		double minCount = Double.MAX_VALUE;
		
		//The entry key is the candidate name and entry value is the vote count
		for(Map.Entry<String, Double> entry : currentVoteCounts.entrySet()) {
			//If the candidate has already won, do not count their votes
			if(!winningCandidates.contains(entry.getKey())) {
				//Check if the vote count for this candidate has exceeded the winning threshold
				if(entry.getValue().compareTo(voteThreshold) > 0) {
					//Mark that a winner was found
					noWinnerFound = false;
					
					//subtract the threshold from the number of votes over
					//Then divide that by the total number of votes. This results in a percentage which acts as
					//a weight for all votes from this winner as they are redistributed. 
					double redistributeAmount = Double.sum(entry.getValue(), voteThreshold*-1.0)/numberOfVotes;
					
					//redistribute the votes of the winner using the calculated redistribution amount
					redistributeVotes(entry.getKey(), redistributeAmount);
					
					//Add the winner to the list of winners and print that they have won
					winningCandidates.add(entry.getKey());
					System.out.println(entry.getKey() + " has won with " + entry.getValue() + " votes.");
					
					//Exit the loop
					break;
				}
			}
			
			if(entry.getValue().compareTo(minCount) < 0) {
				//If this candidate has a lower vote count than the current lowest
				//Store this count as the new minimum
				minCount = entry.getValue();
				//remove all current candidates from the minimum count list
				minCandidatess.clear();
				//and add this candidate to the list
				minCandidatess.add(entry.getKey());
			} else if(entry.getValue().compareTo(minCount) == 0) {
				minCandidatess.add(entry.getKey());
			}
		}
		
		//Since no one won, a loser must be eliminated
		if(noWinnerFound) {
			//If only one candidate has the lowest vote count, eliminate them
			//If 2 or more are tied for the lowest, run a tie breaker and eliminate the loser
			if(minCandidatess.size() == 1) {
				eliminateCandidate(minCandidatess.get(0));
			} else {
				eliminateCandidate(condorcetTieBreak(minCandidatess));
			}
		} 
	}
	
	/**
	 * Perform a condorcet tie break for all factions in the factions list
	 * @param candidates - {@code ArrayList<String>} - The list of candidates to check in the tie breaker
	 * 
	 * @return - String - the name of the candidate that lost the tie breaker
	 */
	private static String condorcetTieBreak(ArrayList<String> candidates) {
		//Output that a tie braker is occuring between the candidates
		System.out.println("Breaking last place tie between: " + String.join(", ", candidates));
		
		//create the master matrix that counts the number of times one candidate beats another candidate
		int[][] masterMatrix = new int[candidates.size()][candidates.size()];
		
		//generate a matrix for each vote and add it to the master
		for(Vote vote : originalVotes) {
			masterMatrix = matrixAddition(masterMatrix, vote.generateMatrix(candidates));
		}
		
		// Create a win matrix which marks if one candidate beats another candidate more in the master matrix
		int[][] winMatrix = new int[candidates.size()][candidates.size()];
		// Create a matrix that stores the magnitude of victories in the win matrix
		int[][] magnitudeMatrix = new int[candidates.size()][candidates.size()];
		
		//loop through the master matrix
		for(int x=0; x<masterMatrix.length; x++) {
			for(int y=0; y<masterMatrix[0].length; y++) {
				// Only 1 half of the matrix is cared about, so skip it if x < y
				if(y>x) {
					if(masterMatrix[x][y] > masterMatrix[y][x]) {
						//If x beat y more than y beat x, mark that x beats y overall in the win matrix
						winMatrix[x][y] = 1;
						//calculate difference of counts in the master and store it as the magnitude
						//The winner will have a positive value while the lose will have a negative value
						magnitudeMatrix[x][y] = masterMatrix[x][y] - masterMatrix[y][x];
						magnitudeMatrix[y][x] = masterMatrix[y][x] - masterMatrix[x][y];
					} else if (masterMatrix[x][y] < masterMatrix[y][x]) {
						//The same thing as above but if Y won
						winMatrix[y][x] = 1;
						magnitudeMatrix[y][x] = masterMatrix[y][x] - masterMatrix[x][y];
						magnitudeMatrix[x][y] = masterMatrix[x][y] - masterMatrix[y][x];
					}
				}
			}
		}
		
		//Create a variable to store the currently lowest win count
		int minWins = Integer.MAX_VALUE;
		//and a list to store the people with the lowest win count
		ArrayList<Integer> winIndecies = new ArrayList<>();
		
		//Loop through each candidate
		for(int i=0; i<candidates.size(); i++) {
			//determine that candidates win number by summing their row in the matrix
			int currWins = matrixRowSum(winMatrix, i);
			
			if(currWins < minWins) {
				//If this candidate has less wins than the current lowest, reset the list and mark this candidate the loser
				minWins = currWins;
				winIndecies.clear();
				winIndecies.add(i);
			} else if (currWins == minWins) {
				//If this candidate has the same number of wins as the lowest, add their index to the list
				winIndecies.add(i);
			}
		}
		
		//If there is someone worse than everyone else, then they lose
		if(winIndecies.size() == 1) {
			return candidates.get(winIndecies.get(0));
		} else {
			//If multiple people share the worst win number, magnitude is taken into account
			int minMagnitude = Integer.MAX_VALUE;
			ArrayList<Integer> worstIndecies = new ArrayList<>();
			
			//Sum the row of the magnitude matrix for each of the worst candidates
			for(Integer rowNum : winIndecies) {
				int currMag = matrixRowSum(magnitudeMatrix, rowNum);
				
				//Keep track of those candidate with the worst magnitude of wins against all opponents in the tie breaker
				if(currMag < minMagnitude) {
					minMagnitude = currMag;
					worstIndecies.clear();
					worstIndecies.add(rowNum);
				} else if(currMag == minMagnitude) {
					worstIndecies.add(rowNum);
				}
			}
			
			//If one is the worst by magnitue, eliminate them
			if(worstIndecies.size() == 1) {
				return candidates.get(worstIndecies.get(0));
			} else {
				//If win number and magnitude did not break the tie, then we look at the original votes for each candidate
				ArrayList<Integer> lowPointIndecies = new ArrayList<>();
				//Points are given for the ranking of a candidate on a ballot, with a higher ranked choice getting more points
				int lowPoint = Integer.MAX_VALUE;
				
				//Loop through the candidates that are stilled tied
				for(Integer i : worstIndecies) {
					int totalPoints = 0;
					
					//get the points for that candidate from each vote
					for(Vote vote : originalVotes) {
						totalPoints += vote.getPoints(candidates.get(i));
					}
					
					//Keep track of the candidates with the worst votes
					if(totalPoints < lowPoint) {
						lowPoint = totalPoints;
						lowPointIndecies.clear();
						lowPointIndecies.add(i);
					} else if (totalPoints == lowPoint) {
						lowPointIndecies.add(i);
					}
				}
				
				//If there is one that is worst, eliminate them
				if(lowPointIndecies.size() == 1) {
					return candidates.get(lowPointIndecies.get(0));
				} else {
					//If there is still a tie, it is broken randomly
					Random rand = new Random();
					
					int returnIndex = rand.nextInt(lowPointIndecies.size());
					
					return candidates.get(lowPointIndecies.get(returnIndex));
				}
			}
		}
	}
	
	/**
	 * Calculate the sum of a row (1st index) in a matrix
	 * 
	 * @param matrix The matrix to operate on
	 * @param row the index of the row to sum
	 * @return int the sum of the row
	 */
	private static int matrixRowSum(int[][] matrix, int row) {
		int sum = 0;
		
		for(int y=0; y<matrix[0].length; y++) {
			sum += matrix[row][y];
		}
		
		return sum;
	}
	
	/**
	 * Add two matrices together
	 * 
	 * @param a the first matrix to add
	 * @param b the second matrix to add
	 * @return int[][] the matrix creating by adding a and b
	 */
	private static int[][] matrixAddition(int[][] a, int[][] b) {
		//get the row and column count for the size of the new matrix
		int rows = a.length;
		int columns = a[0].length;
		
		//create the matrix
		int[][] c = new int[rows][columns];
		
		//perform the addition
		for(int i=0; i<rows; i++) {
			for(int j=0; j<columns; j++) {
				c[i][j] = a[i][j] + b[i][j];
			}
		}
		
		return c;
	}
	
	/**
	 * Perform the initial count of the votes
	 */
	private static void countVotes() {
		
		for(Vote vote : votes) {
			if(!vote.getChoices().isEmpty()) {
				//ensure that the current choice has been reset
				vote.resetCurrentChoice();
				
				String currFaction = vote.getChoice();
				double newCount = currentVoteCounts.get(currFaction)+vote.getWeight();
				currentVoteCounts.put(currFaction, newCount);
			}
		}
	}
	
	/**
	 * Eliminates the specified candidate and redistributes every vote that currently is counting for that candidate with a weight of 1.0
	 * 
	 * @param candidate The candidate to eliminate
	 */
	private static void eliminateCandidate(String candidate) {
		redistributeVotes(candidate, 1.0);
		
		//remove the candidate from the vote counts.
		System.out.println(candidate + " has been eliminated with " + currentVoteCounts.get(candidate) + " votes.");
		currentVoteCounts.remove(candidate);
	}
	
	/**
	 * Redistribute votes from a candidate to that vote's next candidate with a specified weight
	 * 
	 * @param candidate The candidate to redistribute votes from
	 * @param redistributeAmount The weight of the redistributed votes
	 */
	private static void redistributeVotes(String candidate, double redistributeAmount) {
		for(Vote vote : votes) {
			
			//skip the vote if it has no valid candidate choices remaining
			if(vote.getChoices().isEmpty()) {
				continue;
			}
			
			//If the vote is currently counting for the candidate to redistribute from
			if(vote.getChoice().compareTo(candidate) == 0) {
				//If the vote has another valid choice
				if(vote.getChoices().size() > vote.getCurrentChoice()+1) {
					
					//Set the vote to the next candidate and count the vote for that candidate
					vote.alterWeight(redistributeAmount);
					vote.incrementCurrentChoice();
					String newCandidate = vote.getChoice();
					double newCount =  Double.sum(currentVoteCounts.get(newCandidate), vote.getWeight());
					currentVoteCounts.put(newCandidate, newCount);
				}
			}
		}
		
		for(Vote vote : votes) {
			vote.eliminateRanking(candidate);
		}	
	}
}
