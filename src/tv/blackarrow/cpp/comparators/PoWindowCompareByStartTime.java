//
// Copyright © 2012 BlackArrow, Inc. All rights reserved.
//
// The information contained herein is confidential, proprietary to BlackArrow Inc., and
// considered a trade secret as defined in section 499C of the penal code of the State of
// California. Use of this information by anyone other than authorized employees of
// BlackArrow Inc. is granted only under a written non-disclosure agreement, expressly
// prescribing the scope and manner of such use.
//
// $Change$
// $Author$
// $Id$
// $DateTime$
//

package tv.blackarrow.cpp.comparators;

import java.util.Comparator;

import tv.blackarrow.cpp.po.PoWindow;

public class PoWindowCompareByStartTime implements Comparator<PoWindow> {

	@Override
	public int compare(final PoWindow window1, final PoWindow window2) {
		return window1.getStartTime().compareTo(window2.getStartTime());
	}

}
