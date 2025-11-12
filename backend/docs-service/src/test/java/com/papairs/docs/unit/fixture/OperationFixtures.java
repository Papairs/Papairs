package com.papairs.docs.unit.fixture;

import com.papairs.docs.model.OT.DeleteOp;
import com.papairs.docs.model.OT.InsertOp;

public class OperationFixtures {
    public static InsertOp createInsert(int pos, String text, String clientId, String opId) {
        InsertOp op = new InsertOp();
        op.pos = pos;
        op.text = text;
        op.clientId = clientId;
        op.opId = opId;
        op.baseVersion = 0;
        return op;
    }

    public static DeleteOp createDelete(int pos, int length, String clientId, String opId) {
        DeleteOp op = new DeleteOp();
        op.pos = pos;
        op.length = length;
        op.clientId = clientId;
        op.opId = opId;
        op.baseVersion = 0;
        return op;
    }
}
