# Ranked Choice Voting

This program is an implementation of ranked choice voting with a Condorcet tiebreaker implemented in Java. 

## Dependencies

This tool was created using Java 11. It has not been tested on other versions, but should be able to run in anything after or including Java 9.

## Method

A [description](https://www.fairvote.org/rcv#how_rcv_works) and [example](https://www.fairvote.org/multi_winner_rcv_example) of ranked choice voting can be found on FairVote.org. This program implements the process as descriped there. Tiebreakers for ranked choice voting are less standardized, and this tool implements the Condorcet method. A description of the tiebreaking process can be found [here](https://en.wikipedia.org/wiki/Condorcet_method#Pairwise_counting_and_matrices).

## Usage

The tool, VoteCounter, is a console application that requires 3 arguments: 
1. A text file of all candidates. Each candidate should be on a new line with no delimiters included. 
2. A csv file of all votes. A header line should be included, with the fields being Voter Name, Choice 1, Choice 2, Choice 3, ...
   1. A line must have the voter name, though no actual choices need be provided. In addition, different votes may have a different number of provided choices
3. An integer representing the number of winners. This should be at least 1 and no more than the number of candidates.

As an example, `java VoteCounter "candidates.txt" "votes.txt" 3` would take the candidates from `candidates.txt`, the votes from `votes.txt`, and then determine the three winners. The included candidates and votes text files are examples of the required formats for the input files. 

Each analysis pass output updates to the console. The beginning of the round outputs the candidates that remain and their vote counts. If a tiebreaker occurs, it will output which candidates are involved. Finally, it will print the winner or loser that was found in the round with their vote count at that time. After redistributing votes, the next round starts. 