package org.xbib.net.path.spring;

import org.xbib.net.path.spring.element.CaptureTheRestPathElement;
import org.xbib.net.path.spring.element.CaptureVariablePathElement;
import org.xbib.net.path.spring.element.LiteralPathElement;
import org.xbib.net.path.spring.element.PathElement;
import org.xbib.net.path.spring.element.RegexPathElement;
import org.xbib.net.path.spring.element.SeparatorPathElement;
import org.xbib.net.path.spring.element.SingleCharWildcardedPathElement;
import org.xbib.net.path.spring.element.WildcardPathElement;
import org.xbib.net.path.spring.element.WildcardTheRestPathElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Parser for URI path patterns producing {@link PathPattern} instances that can
 * then be matched to requests.
 *
 * <p>The {@link PathPatternParser} and {@link PathPattern} are specifically
 * designed for use with HTTP URL paths in web applications where a large number
 * of URI path patterns, continuously matched against incoming requests,
 * motivates the need for efficient matching.
 *
 * <p>For details of the path pattern syntax see {@link PathPattern}.
 *
 */
public class PathPatternParser {

	/**
	 * Shared, read-only instance of {@code PathPatternParser}. Uses default settings:
	 * <ul>
	 * <li>{@code matchOptionalTrailingSeparator=true}
	 * <li>{@code caseSensitivetrue}
	 * <li>{@code pathOptions=PathContainer.Options.HTTP_PATH}
	 * </ul>
	 */
	public final static PathPatternParser defaultInstance = new PathPatternParser() {

		@Override
		public void setMatchOptionalTrailingSeparator(boolean matchOptionalTrailingSeparator) {
			raiseError();
		}

		@Override
		public void setCaseSensitive(boolean caseSensitive) {
			raiseError();
		}

		@Override
		public void setPathOptions(PathContainer.Options pathOptions) {
			raiseError();
		}

		private void raiseError() {
			throw new UnsupportedOperationException("This is a read-only, shared instance that cannot be modified");
		}
	};

	private boolean matchOptionalTrailingSeparator = true;

	private boolean caseSensitive = true;

	private PathContainer.Options pathOptions = PathContainer.Options.HTTP_PATH;

	// The input data for parsing
	private char[] pathPatternData = new char[0];

	// The length of the input data
	private int pathPatternLength;

	// Current parsing position
	int pos;

	// How many ? characters in a particular path element
	private int singleCharWildcardCount;

	// Is the path pattern using * characters in a particular path element
	private boolean wildcard = false;

	// Is the construct {*...} being used in a particular path element
	private boolean isCaptureTheRestVariable = false;

	// Has the parser entered a {...} variable capture block in a particular
	// path element
	private boolean insideVariableCapture = false;

	// How many variable captures are occurring in a particular path element
	private int variableCaptureCount = 0;

	// Start of the most recent path element in a particular path element
	private int pathElementStart;

	// Start of the most recent variable capture in a particular path element
	private int variableCaptureStart;

	// Variables captures in this path pattern
	private List<String> capturedVariableNames;

	// The head of the path element chain currently being built
	private PathElement headPE;

	// The most recently constructed path element in the chain
	private PathElement currentPE;

	public PathPatternParser() {
	}

	public boolean match(String pattern, String string) {
		return parse(pattern).matches(PathContainer.parsePath(string));
	}

	/**
	 * Whether a {@link PathPattern} produced by this parser should
	 * automatically match request paths with a trailing slash.
	 *
	 * If set to {@code true} a {@code PathPattern} without a trailing slash
	 * will also match request paths with a trailing slash. If set to
	 * {@code false} a {@code PathPattern} will only match request paths with
	 * a trailing slash.
	 *
	 * The default is {@code true}.
	 */
	public void setMatchOptionalTrailingSeparator(boolean matchOptionalTrailingSeparator) {
		this.matchOptionalTrailingSeparator = matchOptionalTrailingSeparator;
	}

	/**
	 * Whether optional trailing slashing match is enabled.
	 */
	public boolean isMatchOptionalTrailingSeparator() {
		return this.matchOptionalTrailingSeparator;
	}

	/**
	 * Whether path pattern matching should be case-sensitive.
	 *
	 * The default is {@code true}.
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Whether case-sensitive pattern matching is enabled.
	 */
	public boolean isCaseSensitive() {
		return this.caseSensitive;
	}

	/**
	 * Set options for parsing patterns. These should be the same as the
	 * options used to parse input paths.
	 * {@code org.springframework.http.server.PathContainer.Options#HTTP_PATH}
	 * is used by default.
	 */
	public void setPathOptions(PathContainer.Options pathOptions) {
		this.pathOptions = pathOptions;
	}

	/**
	 * Return the {@link #setPathOptions configured} pattern parsing options.
	 */
	public PathContainer.Options getPathOptions() {
		return this.pathOptions;
	}

	/**
	 * Process the path pattern content, a character at a time, breaking it into
	 * path elements around separator boundaries and verifying the structure at each
	 * stage. Produces a PathPattern object that can be used for fast matching
	 * against paths.
	 * @param pathPattern the input path pattern, e.g. /project/{name}
	 * @return a PathPattern for quickly matching paths against request paths
	 * @throws PatternParseException in case of parse errors
	 */
	public PathPattern parse(String pathPattern) throws PatternParseException {
		this.pathPatternData = pathPattern.toCharArray();
		this.pathPatternLength = this.pathPatternData.length;
		this.headPE = null;
		this.currentPE = null;
		this.capturedVariableNames = null;
		this.pathElementStart = -1;
		this.pos = 0;
		resetPathElementState();

		while (this.pos < this.pathPatternLength) {
			char ch = this.pathPatternData[this.pos];
			char separator = this.getPathOptions().separator();
			if (ch == separator) {
				if (this.pathElementStart != -1) {
					pushPathElement(createPathElement());
				}
				if (peekDoubleWildcard()) {
					pushPathElement(new WildcardTheRestPathElement(this.pos, separator));
					this.pos += 2;
				}
				else {
					pushPathElement(new SeparatorPathElement(this.pos, separator));
				}
			}
			else {
				if (this.pathElementStart == -1) {
					this.pathElementStart = this.pos;
				}
				if (ch == '?') {
					this.singleCharWildcardCount++;
				}
				else if (ch == '{') {
					if (this.insideVariableCapture) {
						throw new PatternParseException(this.pos, this.pathPatternData,
								PatternParseException.PatternMessage.ILLEGAL_NESTED_CAPTURE);
					}
					// If we enforced that adjacent captures weren't allowed,
					// this would do it (this would be an error: /foo/{bar}{boo}/)
					// } else if (pos > 0 && pathPatternData[pos - 1] == '}') {
					// throw new PatternParseException(pos, pathPatternData,
					// PatternMessage.CANNOT_HAVE_ADJACENT_CAPTURES);
					this.insideVariableCapture = true;
					this.variableCaptureStart = this.pos;
				}
				else if (ch == '}') {
					if (!this.insideVariableCapture) {
						throw new PatternParseException(this.pos, this.pathPatternData,
								PatternParseException.PatternMessage.MISSING_OPEN_CAPTURE);
					}
					this.insideVariableCapture = false;
					if (this.isCaptureTheRestVariable && (this.pos + 1) < this.pathPatternLength) {
						throw new PatternParseException(this.pos + 1, this.pathPatternData,
								PatternParseException.PatternMessage.NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST);
					}
					this.variableCaptureCount++;
				}
				else if (ch == ':') {
					if (this.insideVariableCapture && !this.isCaptureTheRestVariable) {
						skipCaptureRegex();
						this.insideVariableCapture = false;
						this.variableCaptureCount++;
					}
				}
				else if (ch == '*') {
					if (this.insideVariableCapture && this.variableCaptureStart == this.pos - 1) {
						this.isCaptureTheRestVariable = true;
					}
					this.wildcard = true;
				}
				// Check that the characters used for captured variable names are like java identifiers
				if (this.insideVariableCapture) {
					if ((this.variableCaptureStart + 1 + (this.isCaptureTheRestVariable ? 1 : 0)) == this.pos &&
							!Character.isJavaIdentifierStart(ch)) {
						throw new PatternParseException(this.pos, this.pathPatternData,
								PatternParseException.PatternMessage.ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR,
								Character.toString(ch));

					}
					else if ((this.pos > (this.variableCaptureStart + 1 + (this.isCaptureTheRestVariable ? 1 : 0)) &&
							!Character.isJavaIdentifierPart(ch) && ch != '-')) {
						throw new PatternParseException(this.pos, this.pathPatternData,
								PatternParseException.PatternMessage.ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR,
								Character.toString(ch));
					}
				}
			}
			this.pos++;
		}
		if (this.pathElementStart != -1) {
			pushPathElement(createPathElement());
		}
		return new PathPattern(pathPattern, this, this.headPE);
	}

	/**
	 * Just hit a ':' and want to jump over the regex specification for this
	 * variable. pos will be pointing at the ':', we want to skip until the }.
	 *
	 * Nested {...} pairs don't have to be escaped: <tt>/abc/{var:x{1,2}}/def</tt>
	 *
	 * An escaped } will not be treated as the end of the regex: <tt>/abc/{var:x\\{y:}/def</tt>
	 *
	 * A separator that should not indicate the end of the regex can be escaped:
	 */
	private void skipCaptureRegex() {
		this.pos++;
		int regexStart = this.pos;
		int curlyBracketDepth = 0;
		boolean previousBackslash = false;
		while (this.pos < this.pathPatternLength) {
			char ch = this.pathPatternData[this.pos];
			if (ch == '\\' && !previousBackslash) {
				this.pos++;
				previousBackslash = true;
				continue;
			}
			if (ch == '{' && !previousBackslash) {
				curlyBracketDepth++;
			}
			else if (ch == '}' && !previousBackslash) {
				if (curlyBracketDepth == 0) {
					if (regexStart == this.pos) {
						throw new PatternParseException(regexStart, this.pathPatternData,
								PatternParseException.PatternMessage.MISSING_REGEX_CONSTRAINT);
					}
					return;
				}
				curlyBracketDepth--;
			}
			if (ch == this.getPathOptions().separator() && !previousBackslash) {
				throw new PatternParseException(this.pos, this.pathPatternData,
						PatternParseException.PatternMessage.MISSING_CLOSE_CAPTURE);
			}
			this.pos++;
			previousBackslash = false;
		}

		throw new PatternParseException(this.pos - 1, this.pathPatternData,
				PatternParseException.PatternMessage.MISSING_CLOSE_CAPTURE);
	}

	/**
	 * After processing a separator, a quick peek whether it is followed by
	 * a double wildcard (and only as the last path element).
	 */
	private boolean peekDoubleWildcard() {
		if ((this.pos + 2) >= this.pathPatternLength) {
			return false;
		}
		if (this.pathPatternData[this.pos + 1] != '*' || this.pathPatternData[this.pos + 2] != '*') {
			return false;
		}
		char separator = this.getPathOptions().separator();
		if ((this.pos + 3) < this.pathPatternLength && this.pathPatternData[this.pos + 3] == separator) {
			throw new PatternParseException(this.pos, this.pathPatternData,
					PatternParseException.PatternMessage.NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST);
		}
		return (this.pos + 3 == this.pathPatternLength);
	}

	/**
	 * Push a path element to the chain being build.
	 * @param newPathElement the new path element to add
	 */
	private void pushPathElement(PathElement newPathElement) {
		if (newPathElement instanceof CaptureTheRestPathElement) {
			// There must be a separator ahead of this thing
			// currentPE SHOULD be a SeparatorPathElement
			if (this.currentPE == null) {
				this.headPE = newPathElement;
				this.currentPE = newPathElement;
			}
			else if (this.currentPE instanceof SeparatorPathElement) {
				PathElement peBeforeSeparator = this.currentPE.prev;
				if (peBeforeSeparator == null) {
					// /{*foobar} is at the start
					this.headPE = newPathElement;
					newPathElement.prev = null;
				}
				else {
					peBeforeSeparator.next = newPathElement;
					newPathElement.prev = peBeforeSeparator;
				}
				this.currentPE = newPathElement;
			}
			else {
				throw new IllegalStateException("Expected SeparatorPathElement but was " + this.currentPE);
			}
		}
		else {
			if (this.headPE == null) {
				this.headPE = newPathElement;
				this.currentPE = newPathElement;
			}
			else if (this.currentPE != null) {
				this.currentPE.next = newPathElement;
				newPathElement.prev = this.currentPE;
				this.currentPE = newPathElement;
			}
		}
		resetPathElementState();
	}

	private char[] getPathElementText() {
		int len = Math.min(this.pathPatternLength, this.pos - this.pathElementStart);
		char[] pathElementText = new char[len];
		System.arraycopy(this.pathPatternData, this.pathElementStart, pathElementText, 0, len);
		return pathElementText;
	}

	/**
	 * Used the knowledge built up whilst processing since the last path element to determine what kind of path
	 * element to create.
	 * @return the new path element
	 */
	private PathElement createPathElement() {
		if (this.insideVariableCapture) {
			throw new PatternParseException(this.pos, this.pathPatternData, PatternParseException.PatternMessage.MISSING_CLOSE_CAPTURE);
		}
		PathElement newPE = null;
		char separator = this.getPathOptions().separator();
		if (this.variableCaptureCount > 0) {
			if (this.variableCaptureCount == 1 && this.pathElementStart == this.variableCaptureStart &&
					this.pathPatternData[this.pos - 1] == '}') {
				if (this.isCaptureTheRestVariable) {
					// It is {*....}
					newPE = new CaptureTheRestPathElement(
							this.pathElementStart, getPathElementText(), separator);
				}
				else {
					// It is a full capture of this element (possibly with constraint), for example: /foo/{abc}/
					try {
						newPE = new CaptureVariablePathElement(this.pathElementStart, getPathElementText(),
								this.isCaseSensitive(), separator);
					}
					catch (PatternSyntaxException pse) {
						throw new PatternParseException(pse,
								findRegexStart(this.pathPatternData, this.pathElementStart) + pse.getIndex(),
								this.pathPatternData, PatternParseException.PatternMessage.REGEX_PATTERN_SYNTAX_EXCEPTION);
					}
					recordCapturedVariable(this.pathElementStart,
							((CaptureVariablePathElement) newPE).getVariableName());
				}
			}
			else {
				if (this.isCaptureTheRestVariable) {
					throw new PatternParseException(this.pathElementStart, this.pathPatternData,
							PatternParseException.PatternMessage.CAPTURE_ALL_IS_STANDALONE_CONSTRUCT);
				}
				RegexPathElement newRegexSection = new RegexPathElement(this.pathElementStart,
						getPathElementText(), this.isCaseSensitive(),
						this.pathPatternData, separator);
				for (String variableName : newRegexSection.getVariableNames()) {
					recordCapturedVariable(this.pathElementStart, variableName);
				}
				newPE = newRegexSection;
			}
		}
		else {
			if (this.wildcard) {
				if (this.pos - 1 == this.pathElementStart) {
					newPE = new WildcardPathElement(this.pathElementStart, separator);
				}
				else {
					newPE = new RegexPathElement(this.pathElementStart, getPathElementText(),
							this.isCaseSensitive(), this.pathPatternData, separator);
				}
			}
			else if (this.singleCharWildcardCount != 0) {
				newPE = new SingleCharWildcardedPathElement(this.pathElementStart, getPathElementText(),
						this.singleCharWildcardCount, this.isCaseSensitive(), separator);
			}
			else {
				newPE = new LiteralPathElement(this.pathElementStart, getPathElementText(),
						this.isCaseSensitive(), separator);
			}
		}
		return newPE;
	}

	/**
	 * For a path element representing a captured variable, locate the constraint pattern.
	 * Assumes there is a constraint pattern.
	 * @param data a complete path expression, e.g. /aaa/bbb/{ccc:...}
	 * @param offset the start of the capture pattern of interest
	 * @return the index of the character after the ':' within
	 * the pattern expression relative to the start of the whole expression
	 */
	private int findRegexStart(char[] data, int offset) {
		int pos = offset;
		while (pos < data.length) {
			if (data[pos] == ':') {
				return pos + 1;
			}
			pos++;
		}
		return -1;
	}

	/**
	 * Reset all the flags and position markers computed during path element processing.
	 */
	private void resetPathElementState() {
		this.pathElementStart = -1;
		this.singleCharWildcardCount = 0;
		this.insideVariableCapture = false;
		this.variableCaptureCount = 0;
		this.wildcard = false;
		this.isCaptureTheRestVariable = false;
		this.variableCaptureStart = -1;
	}

	/**
	 * Record a new captured variable. If it clashes with an existing one then report an error.
	 */
	private void recordCapturedVariable(int pos, String variableName) {
		if (this.capturedVariableNames == null) {
			this.capturedVariableNames = new ArrayList<>();
		}
		if (this.capturedVariableNames.contains(variableName)) {
			throw new PatternParseException(pos, this.pathPatternData,
					PatternParseException.PatternMessage.ILLEGAL_DOUBLE_CAPTURE, variableName);
		}
		this.capturedVariableNames.add(variableName);
	}

}
