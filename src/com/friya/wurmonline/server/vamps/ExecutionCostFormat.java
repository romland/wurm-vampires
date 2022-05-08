package com.friya.wurmonline.server.vamps;

import java.text.DecimalFormat;
import java.text.FieldPosition;

@SuppressWarnings("serial")
public class ExecutionCostFormat extends DecimalFormat
{
	@Override
	public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition)
	{
		Mod.totalExecutionCost += number;

		return super.format(number, result, fieldPosition);
	}
}
