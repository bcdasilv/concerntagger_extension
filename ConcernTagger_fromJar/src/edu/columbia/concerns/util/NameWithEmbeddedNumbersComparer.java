package edu.columbia.concerns.util;

import java.util.Comparator;

import edu.columbia.concerns.repository.Concern;

public class NameWithEmbeddedNumbersComparer implements Comparator<Concern>
{
	public static boolean doesNameStartWithDigit(String concernName)
	{
		return concernName.length() > 0 && Character.isDigit(concernName.charAt(0));
	}
	
	@Override
	public int compare(Concern lhs, Concern rhs)
	{
		return compareTo(lhs, rhs);
	}

	static public int compareTo(Concern lhs, Concern rhs)
	{
		if (lhs == rhs)
			return 0;
		else if (lhs == null)
			return -1;
		else if (rhs == null)
			return +1;

		String lhsName = lhs.getName();
		String rhsName = rhs.getName();

		boolean lhsStartsWithDigit = doesNameStartWithDigit(lhsName);
		boolean rhsStartsWithDigit = doesNameStartWithDigit(rhsName);

		if (lhsStartsWithDigit && rhsStartsWithDigit)
		{
			return embeddedNumberCompareTo(lhsName, rhsName);
		}
		else if (lhsStartsWithDigit)
		{
			// All numbered concerns come before non-numbered concerns
			return -1;
		}
		else if (rhsStartsWithDigit)
		{
			// All numbered concerns come before non-numbered concerns
			return +1;
		}
		else
		{
			return lhs.compareTo(rhs);
		}
	}

	static public int embeddedNumberCompareTo(String lhs, String rhs)
	{
		if (lhs == rhs)
			return 0;
		else if (lhs == null)
			return -1;
		else if (rhs == null)
			return +1;

		int lenLhs = lhs.length();
		int lenRhs = rhs.length();

		int indexLhs = 0;
		int indexRhs = 0;

		while(indexLhs < lenLhs && indexRhs < lenRhs)
		{
			StringBuffer numStrLhs = null;
			StringBuffer numStrRhs = null;
			
			int numLenLhs = 0;
			int numLenRhs = 0;

			char cLhs = lhs.charAt(indexLhs);
			char cRhs = rhs.charAt(indexRhs);
			
			// If we hit a digit find out where the last digit is
			while (Character.isDigit(cLhs))
			{
				if (numStrLhs == null)
					numStrLhs = new StringBuffer();

				numStrLhs.append(cLhs);
				++numLenLhs;
				cLhs = lhs.charAt(indexLhs + numLenLhs);
			}

			while (Character.isDigit(cRhs))
			{
				if (numStrRhs == null)
					numStrRhs = new StringBuffer();

				numStrRhs.append(cRhs);
				++numLenRhs;
				cRhs = rhs.charAt(indexRhs + numLenRhs);
			}

			// If we encounter numbers in both strings then do a
			// number comparison
			if (numStrLhs != null && numStrRhs != null)
			{
				assert numStrLhs.length() == numLenLhs;
				assert numStrRhs.length() == numLenRhs;

				int numLhs = Integer.parseInt(numStrLhs.toString());
				int numRhs = Integer.parseInt(numStrRhs.toString());

				// Sanity check: We should have ignored the minus sign
				assert numLhs >= 0 && numRhs >= 0;

				int cmp = numLhs - numRhs;
				if (cmp != 0)
				{
					// Numbers are different. Ex: 5 and 8
					return cmp;
				}
				else if (numLenLhs != numLenRhs)
				{
					// Numbers are equivalent but have different
					// lengths. Ex: 5 and 005
					return numLenLhs - numLenRhs;
				}
				else
				{
					// Numbers are exactly the same. Skip past them.
					indexLhs += numLenLhs;
					indexRhs += numLenRhs;
				}
			}

			// Otherwise, compare the characters as usual
			else if (cLhs != cRhs)
			{
				return cLhs - cRhs;
			}

			// Characters are equal. Skip to the next one.
			else
			{
				++indexLhs;
				++indexRhs;
			}
		}

		// Strings are the same up to the now but we've reached
		// the end of one or both of them
		
		if (indexLhs == lenLhs && indexRhs == lenRhs)		// "Marc" == "Marc"
			return 0;
		else if (indexLhs == lenLhs) 				// "Marc" == "Marcy" 
			return -rhs.charAt(indexRhs);
		else //if (r == rhsLen)				// "Marcy" == "Marc"
			return +lhs.charAt(indexLhs);
	}
}
