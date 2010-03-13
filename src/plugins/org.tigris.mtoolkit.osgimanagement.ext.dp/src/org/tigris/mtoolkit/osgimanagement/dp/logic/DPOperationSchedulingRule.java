package org.tigris.mtoolkit.osgimanagement.dp.logic;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public class DPOperationSchedulingRule implements ISchedulingRule {

	private Framework fw;
	
	public DPOperationSchedulingRule(Framework fw) {
		Assert.isNotNull(fw);
		this.fw = fw;
	}

	public boolean contains(ISchedulingRule rule) {
		if (rule == this)
			return true;
		return false;
	}

	public boolean isConflicting(ISchedulingRule rule) {
		if (!(rule instanceof DPOperationSchedulingRule))
			return false;
		return ((DPOperationSchedulingRule) rule).fw == fw;
	}

}
