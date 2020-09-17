package tv.blackarrow.cpp.model.scte224.asserts;

import com.google.gson.annotations.Expose;

public abstract class BaseAssert implements Assert {
	
	@Expose
	protected AssertOperation assertOperation = AssertOperation.EQUALS;
	
	protected BaseAssert() {}
	
	protected BaseAssert(AssertOperation assertOperation) {
		this();
		this.assertOperation = assertOperation;
	}

	public boolean isTrue(Object firstObject, Object secondObject) {
		return this.assertOperation.execute(firstObject, secondObject);
	}

}
