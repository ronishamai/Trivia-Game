package il.ac.tau.cs.sw1.trivia;

import java.util.HashSet;
import java.util.Set;

public class Question {

	private String question;
	private Set<String> answers = new HashSet<String>();
	private String rightAnswer;

	public Question(String line) throws RuntimeException {
		if (line == "" || line == null) {
			throw new RuntimeException("invalid question line");
		}

		String[] tokens = line.split("\t");
		if (tokens.length != 5) {
			throw new RuntimeException("invalid question line");
		}

		this.question = tokens[0];
		this.rightAnswer = tokens[1];
		for (int i = 1; i < 5; i++) {
			this.answers.add(tokens[i]);
		}
	}

	public String getQuestion() {
		return this.question;
	}

	public Set<String> getAnsweres() {
		return this.answers;
	}

	public boolean isCorrectAnswer(String answer) {
		return this.rightAnswer.equals(answer);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((answers == null) ? 0 : answers.hashCode());
		result = prime * result + ((question == null) ? 0 : question.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Question other = (Question) obj;
		if (answers == null) {
			if (other.answers != null)
				return false;
		} else if (!answers.equals(other.answers))
			return false;
		if (question == null) {
			if (other.question != null)
				return false;
		} else if (!question.equals(other.question))
			return false;
		return true;
	}

}
