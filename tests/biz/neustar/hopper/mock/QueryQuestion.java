package biz.neustar.hopper.mock;

import biz.neustar.hopper.record.Record;

/**
 * AN XFR question: qname, qclass, qtype and perhaps IXFR SOA.
 * 
 * @author mkube
 * 
 */
public class QueryQuestion {

	private final Record Question;
	private final Long ixfrSoa;

	public QueryQuestion(Record question, Long ixfrSoa) {
		this.Question = question;
		this.ixfrSoa = ixfrSoa;

	}

	public QueryQuestion(Record question) {
		this(question, 0L);
	}

	protected Long getIxfrSoa() {
		return ixfrSoa;
	}

	protected Record getQuestion() {
		return Question;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Question == null) ? 0 : Question.hashCode());
		result = prime * result + ((ixfrSoa == null) ? 0 : ixfrSoa.hashCode());
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
		QueryQuestion other = (QueryQuestion) obj;
		if (Question == null) {
			if (other.Question != null)
				return false;
		} else if (!Question.equals(other.Question))
			return false;
		if (ixfrSoa == null) {
			if (other.ixfrSoa != null)
				return false;
		} else if (!ixfrSoa.equals(other.ixfrSoa))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "QueryQuestion [Question=" + Question + ", ixfrSoa=" + ixfrSoa + "]";
	}

}
