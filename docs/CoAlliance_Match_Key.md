
Colorado Alliance MARC record match key generation.
August-14-2026 (_v08142026)

What's new in _v08142026:
	Two remaining ways a key could be off-spec are closed. Both were
	reported by Ed Summers (Stanford University Libraries / POD) from the
	same 39.4 million record corpus that produced the _v07312026 fixes.

		- Turkish capital I with dot above ("İ", U+0130) no longer widens
		  the key. The whole key is converted to lowercase, and this one
		  character lowercases into TWO characters — "i" plus a combining
		  dot above. Because each section used to be padded to its width
		  BEFORE that conversion, an "İ" counted as one character while
		  padding and took up two afterwards, pushing the key to 189, 190
		  or 191 characters. Sections are now converted to lowercase
		  before they are padded, so widths are measured on the final
		  characters. "İ" is the only character in Unicode whose lowercase
		  form is longer than the original, so no other character is
		  affected. This reached the key through the Publisher Name
		  section and affected 10,575 of 39.4 million records, all Turkish
		  or Ottoman.

		- The publication year of a government document is now validated
		  before it is used. When an 086 $a was present the year was taken
		  from control field 008 characters 7-10 without any of the checks
		  applied elsewhere. A malformed value there produced a 2- or
		  3-character year and shortened the whole key, and the placeholder
		  "9999" (meaning "unknown year") was used as if it were a real
		  year even when characters 11-14 held a valid one. That second
		  case produced a normal-length key that quietly could not match:
		  the same record without an 086 got the right year. Government
		  documents still prefer characters 7-10, but only when that value
		  is a real year; otherwise they now follow the same path as every
		  other record.

	NOTE: a key ending in "_v07312026" is NOT comparable to one ending in
	"_v08142026" for government documents or for records whose publisher
	contains "İ". All other records produce the same key under both
	versions. Records must be reindexed to carry the new key.

Previous version changes (07-31-26):
	Every match key is now exactly 188 characters.

	Two sections could previously collapse to zero width instead of being
	padded, which produced short keys:
		- Title: a record with no 245 field at all (for example a MARC
		  holdings record, Leader/06 = 'x') emitted nothing instead of
		  95 underscores.
		- Publisher Name: a record with no 264 $b and no 260 $b emitted
		  nothing instead of 5 underscores.
	Keys could therefore be 188, 183, 93 or 88 characters long. Both cases
	now pad to their full section width, as this document has always
	specified. Reported by Ed Summers (Stanford University Libraries / POD),
	who found that 7% of 39 million records produced off-length keys.
	See https://github.com/co-alliance/coalliance-matchkey/issues/1

	NOTE: a key ending in "_v03182026" is NOT comparable to one ending in
	"_v07312026" for records that lack a 245 or a publisher. Records must be
	reindexed to carry the new key.

Previous version changes (03-18-26):
	A version string "_v03182026" is now appended to the matchkey just
	before the format character. This allows the system to distinguish
	which algorithm version produced a given matchkey in the Solr index.

Previous version changes (02-03-26):
	Title field expanded from 70 to 95 characters.
	Government Document Number (086 $a) removed for HATHI compatibility.

The Gold Rush system allows libraries to do in-depth analytics for their cataloged library holdings for either themselves or in comparison to other libraries that are already in the system. It supports a wide variety of use cases including shared print, weeding, collection building, space planning, storage facilities, etc. Due to the wide variety of cataloging sources and the long bibliographic history of most collections, no single number (e.g. OCLC, ISBN) can be used for matching purposes. This match key uses a variety of elements from the MARC record to bring together common bibliographic records. No key is perfect so this particular approach is periodically being modified to improve matching within the system. Many other systems use their own matching algorithms to accomplish the same purpose and some of these have been consulted in building this algorithm.

These MARC fields are used:

Title
	Combine 245 $a $b $p (first occurrence)

General Media Description
	245 $h  **DISABLED as of 11-15-22** — always returns 5 underscores "_____"

Publication Year
	First check control field 008: if character 6 equals 'r' retrieve reissue date from characters 7-10;
	if there is a government document (086$a exists) use the date from characters 7-10 when it is a
	valid year (4 digits and >= 1200 and != 9999), otherwise treat the record like any other;
	then use date from characters 11-14 (if valid and >= 1200 and != 9999), falling back to 7-10.
	If no valid date found, check 264$c then 260$c. For 264$c and 260$c, get the rightmost
	4 digits giving precedence to years preceded by 'c'. If still no valid date found return "0000".
	NOTE: character count starts at 0.

Pagination
	300 $a

Edition Statement
	250 $a — if the field is empty and the format is "Book" mark this as a 1st edition "1__".

Publisher Name
	First check 264 $b then 260 $b. Ampersands are removed, then diacritics and
	non-alphanumeric characters (other than spaces) are automatically removed via
	stripPuncuation(). Remove underscores, normalize accents, convert to lowercase,
	and pad with underscores to 5 characters.

Type of
	Character #6 of Leader

Title Part
	245 $p all multi-valued $p fields

Title Number
	245 $n

Author
	100 $a, 110 $a, 111 $a, 130 $a

Title-Inclusive Dates (used by Daily Camera focus magazine)
	245 $f

Government Document Number
	086 $a — **REMOVED from match key as of 02-03-26** for HATHI compatibility.

Version String
	The version string (currently "_v08142026") is appended to the
	matchkey just before the format character. This identifies which
	algorithm version produced the key.

Electronic indicator
	Check 245 $h, 590 $a, 533 $a, 300 $a, 007, 337 $a, 086+856 combination,
	and MARC filename; appending 'e' if resource is electronic otherwise
	appending 'p'. See getFormatCharacter( Record ) method details for filename usage.


Match Key Section description.

NOTE: Each section is padded with underscores to create the desired character count.
This applies with no exceptions. A section whose source field is absent, empty, or
reduced to nothing by cleaning is still emitted at its full width as underscores, so
every match key is exactly 188 characters. (Through _v03182026 the Title and
Publisher Name sections did not follow this rule; through _v07312026 a Turkish "İ"
in the Publisher Name could still overrun its section — see "What's new" above.)

If a field is multi-valued, take the first value unless specified.
Functions (in bold) are described in appendix.

| Chars | Element | MARC Source | Notes |
|-------|---------|-------------|-------|
| 95 | Title | 245 $a $b $p | Combine $a $b and $p applying strip_punctuation_SPACE() to each, then remove all spaces, trim(), normalizeString(), pad_with_underscores( 95 ). If the record has no 245 field at all (e.g. a MARC holdings record, Leader/06='x'), the section is 95 underscores. **NOTE:** If the first MARC 245$6 field indicates a linked 880 field, the 880 field is used instead. For non-Roman languages without an 880 field, falls back to LLC number, ISBN, or ISSN. |
| 5 | General Media Designation (GMD) | 245 $h | **DISABLED (11-15-22)** — Always returns "_____". Previously: the first five contiguous alphanumeric characters, right-padded with underscores. |
| 4 | Publication Year | 008, 264 $c, 260 $c | First check 008 field (see above for reissue/gov doc logic). If no valid number, check 264$c then 260$c. Get rightmost 4 digits, giving precedence to years preceded by 'c'. If not found return "0000". pad_with_underscores( 4 ) |
| 4 | Pagination | 300 $a | First four contiguous numeric characters are used. If there are fewer than four contiguous numeric characters, or if there is no source field, these bytes are assigned underscores. pad_with_underscores( 4 ) |
| 3 | Edition Statement | 250 $a | First three, first two or first contiguous numeric characters is used. If there are no numeric characters, then the first three, first two or first contiguous alphabetic characters is used. Diacritics are automatically removed from this element. Convert 'fir' to 1, 'sec' to 2, 'thi' to 3, 'for' to 4, 'fif' to 5, 'six' to 6, 'sev' to 7, 'eig' to 8, 'nin' to 9, '10t' to 10. If edition is empty and format is "Book", defaults to "1__" (1st edition). pad_with_underscores( 3 ) |
| 5 | Publisher Name | 264 $b, 260 $b | If 264 is empty check 260$b. Remove ampersands, apply stripPuncuation(), remove underscores, normalize accents. pad_with_underscores( 5 ), which lowercases before it measures. If neither 264$b nor 260$b is present, or the value cleans to nothing, the section is 5 underscores. |
| 1 | Type of | Leader | character #6. If leader contains 10 or more characters return the 6th character of the leader; otherwise the section is a single underscore. |
| 30 | Title Part | 245 $p | If multiple $p subfields, take the first 10 characters of each. trim(), stripPuncuation(), pad_with_underscores( 30 ). **NOTE:** The first $p has already been added to the Title section. |
| 10 | Title Number | 245 $n | stripPuncuation(), pad_with_underscores( 10 ) |
| 5 | Author | 100 $a, 110 $a, 111 $a, 130 $a | Combine the contents of each field applying stripPuncuationAndRemoveAccents() to each. Remove underscores and spaces. pad_with_underscores( 5 ), which lowercases before it measures. |
| 15 | Title-Inclusive Dates | 245 $f | Remove all spaces then stripPuncuation(), padWithUnderscores( 15 ) |
| — | Government Document Number | 086 $a | **REMOVED from match key (02-03-26)** for HATHI compatibility. Previously: stripPuncuationAndRemoveAccents(), trimMAXfieldLength(). |
| 10 | Version String | — | The version string (currently "_v08142026") is appended here, just before the format character. |
| 1 | Format Character | 245 $h, 590 $a, 533 $a, 300 $a, 007, 337 $a, 086+856 | getFormatCharacter() — append 'e' for electronic, otherwise 'p'. |


Post-processing: The entire match key is converted to lowercase (each padded section is
already lowercase as of _v08142026, so this affects only the fixed-width sections), then any remaining
spaces are replaced with underscores.

NOTE: earlier revisions of this document also described colons being replaced with
'x'. That replacement was written into the indexer on 05-16-18 but has never taken
effect (the statement that applied it was immediately overwritten by the space
replacement), so no released match key has ever had it. Colons are already removed
at the section level by stripPuncuation(). The claim is dropped here rather than
implemented, to keep this document a description of what the algorithm actually does.


Appendix - Method definitions

	Example:'EP 1.1/5:' becomes 'EP_1_1_5'

	String stripPuncuation( String input )
	{
		NOTE: .replace() replaces all occurrences.

		input = input.replace( "%22", "_" );
		input = input.replace( "%", "_" );

			// Remove leading 'a' or 'A' and surrounding spaces.
		input = input.replaceFirst( "^[ ]+[aA][ ]+"  , "");

			// Remove leading 'an' or 'An' and surrounding spaces.
		input = input.replaceFirst( "^[ ]+[aA]n[ ]+" , "");

			// Remove leading 'the' or 'The' and surrounding spaces.
		input = input.replaceFirst( "^[ ]+[tT]he[ ]+", "");

		input = input.replace(  "'", "" ); //39
		input = input.replace(  "{", "" ); //123
		input = input.replace(  "}", "" ); //125
		input = input.replace(  "&", "and" ); //38

		input = input.replace(  '\u0020', UNDERSCORE ); //32 ' '
		input = input.replace(  '\u0021', UNDERSCORE ); //33 '!'
		input = input.replace(  '\u0022', UNDERSCORE ); //34 '"'
		input = input.replace(  '\u0023', UNDERSCORE ); //35 '#'
		input = input.replace(  '\u0024', UNDERSCORE ); //36 '$'
		input = input.replace(  '\u0028', UNDERSCORE ); //40 '('
		input = input.replace(  '\u0029', UNDERSCORE ); //41 ')'
		input = input.replace(  '\u002A', UNDERSCORE ); //42 '*'
		input = input.replace(  '\u002B', UNDERSCORE ); //43 '+'
		input = input.replace(  '\u002C', UNDERSCORE ); //44 ','
		input = input.replace(  '\u002D', UNDERSCORE ); //45 '-'
		input = input.replace(  '\u002E', UNDERSCORE ); //46 '.'
		input = input.replace(  '\u002F', UNDERSCORE ); //47 '/'
		input = input.replace(  '\u003A', UNDERSCORE ); //58 ':'
		input = input.replace(  '\u003B', UNDERSCORE ); //59 ';'
		input = input.replace(  '\u003C', UNDERSCORE ); //60 '<'
		input = input.replace(  '\u003D', UNDERSCORE ); //61 '='
		input = input.replace(  '\u003E', UNDERSCORE ); //62 '>'
		input = input.replace(  '\u003F', UNDERSCORE ); //63 '?'
		input = input.replace(  '\u0040', UNDERSCORE ); //64 '@'
		input = input.replace(  '\u005B', UNDERSCORE ); //91 '['
		input = input.replace(  '\\',     UNDERSCORE ); //92 '\'
		input = input.replace(  '\u005D', UNDERSCORE ); //93 ']'
		input = input.replace(  '\u005E', UNDERSCORE ); //94 '^'
		input = input.replace(  '\u005F', UNDERSCORE ); //95 '_'
		input = input.replace(  '\u0060', UNDERSCORE ); //96 '`'
		input = input.replace(  '\u007C', UNDERSCORE ); //124 '|'
		input = input.replace(  '\u007E', UNDERSCORE ); //126 '~'
		input = input.replace(  '\u00A9', UNDERSCORE ); //169 '(c)'

		return result }

*****
* Replace punctuation in a String with spaces
****
String stripPuncuation_SPACE( String input ){

   Same as stripPuncuation() except characters are replaced with spaces
   instead of underscores. This is used for the title to handle punctuation
   at the end of the title:

   EXAMPLE:
	stripPuncuation_SPACE( "The law of primitive man /").trim()

	stripPuncuation_SPACE( "The law of primitive man;").trim()

 	stripPuncuation_SPACE( "The law of primitive man : ").trim()


	All return "The law of primitive man"
}


*****
* Remove leading and trailing whitespace from a String using the standard Java String function.
*****
String String.trim(){

	java.lang.String.trim()
}


*****
*  Replace all spaces in a String with underscores.
*  Trim length of String to desired length. If String is too short pad with underscores.
*****
String padWithUnderscores( String input, Integer count ){

	replace repeating spaces with single space.

	replace all spaces in 'input' with underscores

	convert 'input' to lowercase — done here, before the length is measured, so that
	a character whose lowercase form is longer than itself (Turkish "İ", U+0130, the
	only one in Unicode) cannot overrun the section width. See _v08142026.

	Trim 'input' to length to 'count' characters.

	If needed, pad the end of the string with underscores to create the desired character String.

	return result;
}


*****
* Trim a String to the maximum length allowed for a Solr field. This is used as a precaution, it is
*  usually an error for a MARC field to be of this length.
*****
String trimMAXfieldLength( String input ){

	trim input to the max length of a solr field (32,000)

	return result
}


*****
* Remove all punctuation from a String
*****
String stripPuncuationAndRemoveAccents( String input ){

	tempString =  stripPuncuation( input )

	output =  normalizeStringAndRemoveAccents( tempString )

	return output
}


*****
* Normalize characters represented differently in UTF-16 and UTF-8 to be the same character.
*****
String normalizeString( String input ){

	output = java.text.Normalizer.normalize( input, Form.NFD )

	return output
}


*****
* Normalize and remove Diacritical Marks
*****
String normalizeStringAndRemoveAccents( String input ){

	output =
java.text.Normalizer.normalize( input, Form.NFD ).replaceAll( "\\p{InCombiningDiacriticalMarks}" )

       	return output
}

String getFormatCharacter( Record ){ //Note: this format character is appended to matchKey

	First check for pre RDA...

		if( 245$h contains "electronic resource" OR 590$a contains "electronic reproduction" OR 533$a contains "electronic reproduction" OR 300$a contains "online resource" OR control field 007 starts with 'C' ){
			return 'e'
		}

	Next check for RDA...

		if( 337$a starts with 'c' ){
			return 'e'
		}

	Next check for electronic government document...

		if( 086 not null AND 856 not null ){
			return 'e'
		}

	Next check MARC file name...

		if ( MARC file name contains "electronic" or "ebook" (ignore case) ){
			record 'Electronic' in format field
		}

		if ( MARC file name contains "physical" or "print" (ignore case) )
		{
			remove Electronic from format field
			return p;
		}

	if( format field contains 'Electronic' ){
		return e;
	} otherwise return p;
}


Match Key Example

americancounciloflearnedsocietiesannualreportfortheyears20062007and20052006_____________________2008________distra________________________________________ameri____________________________v08142026e

000 	02801nam a22005052u 4500
001	991034738289702766
005	20170426153237.0
006	m d f
007	cr ||||||||||a
008	080101s2008 xxu|||| st ||| ||eng d
035	|a(CoDU)b49584571-01uode_inst
035	|a(ERIC)ED516193|9ExL
035	|a(MvI) 3N000000499872
040	|aericd|cMvI|dMvI|dCoDU
086	0 |aED 1.310/2:516193x
099	9|aED516193
110	2 |aAmerican Council of Learned Societies.
245	10|aAmerican Council of Learned Societies Annual Report for the Years 2006-2007 and 2005-200	6|h[electronic resource].
260	|a[S.l.] :|bDistributed by ERIC Clearinghouse,|c2008.
300	|a1 online resource (66 p.)
500	|aAvailability: American Council of Learned Societies. 633 Third Avenue 8th Floor, New York, NY 10017. Tel: 212-697-1505; Fax: 212-949-8058; Web site: http://www.acls.org.|5ericd.
500	|aAbstractor: ERIC.|5ericd.
516	|aText (Reports, Descriptive).
520	|aThe American Council of Learned Societies (ACLS) provides the humanities and related social sciences with leadership, opportunities for innovation, and national and international representation. The American Council of Learned Societies was created in 1919 to represent the United States in the Union Academique Internationale. Its mission is "the advancement of humanistic studies in all fields of the humanities and social sciences and the maintenance and strengthening of national societies dedicated to those studies." This paper presents the annual report of ACLS for the years 2005-2006 and 2006-2007.
524	|aAmerican Council of Learned Societies.|2ericd.
650	07|aHumanities.|2ericd.
650	07|aSocial Sciences.|2ericd.
650	07|aEducational Research.|2ericd.
650	07|aAdvocacy.|2ericd.
650	07|aWriting for Publication.|2ericd.
650	07|aFaculty Publishing.|2ericd.
650	07|aScholarship.|2ericd.
650	07|aPeriodicals.|2ericd.
650	07|aLeadership.|2ericd.
650	07|aEducational Innovation.|2ericd.
650	07|aAnnual Reports.|2ericd.
653	0 |aUnited States.
655	7|aReports, Descriptive.|2ericd.
710	2 |aAmerican Council of Learned Societies.
730	0 |aERIC documents (Online)
856	40|uhttp://www.eric.ed.gov/contentdelivery/servlet/ERICServlet?accno=ED516193|zAccess online
907	|a.b49584571|b05-09-16|c08-12-11
945	|aED516193|g1|h908|j0|k0|linter|o-|p$0.00|r-|sj|t0|u0|v0|w0|x0|y.i60300954|z08-12-11
998	|ain|b08-12-11|cm|da|e-|feng|gxxu|h0|i1
946	|y53824704520002766|uhttp://du-primo.hosted.exlibrisgroup.com/openurl/01UODE/01UODE_SERVICES?u.ignore_date_coverage=true&rft.mms_id=991034738289702766|zERIC Collection|853824704520002766


Key breakdown:
- Title (95): "americancounciloflearnedsocietiesannualreportfortheyears20062007and20052006_____________________"
- GMD (5): "_____" (disabled)
- Pub Year (4): "2008"
- Pagination (4): "____"
- Edition (3): "___"
- Publisher (5): "distr"
- Type (1): "a"
- Title Part (30): "______________________________"
- Title Number (10): "__________"
- Author (5): "ameri"
- Title Dates (15): "_______________"
- Version (10): "_v08142026"
- Format (1): "e"
