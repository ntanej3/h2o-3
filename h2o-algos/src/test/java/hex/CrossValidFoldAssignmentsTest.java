package hex;

import hex.genmodel.utils.DistributionFamily;
import hex.tree.gbm.GBM;
import hex.tree.gbm.GBMModel;
import org.junit.BeforeClass;
import org.junit.Test;
import water.DKV;
import water.TestUtil;
import water.fvec.Frame;
import water.fvec.Vec;
import water.rapids.ast.prims.advmath.AstKFold;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CrossValidFoldAssignmentsTest extends TestUtil {
  @BeforeClass() public static void setup() { stall_till_cloudsize(1); }

  // Check that we should not make a copy of fold assignments Vec in ModelBuilder.
  // Fold column as a part of train dataset is not being tracked by Scope.enter() and is still available
  // after the model is trained.
  @Test public void checkFoldAssignmentsAreKeptWithoutMakeCopy() {
    final int nfolds = 3;
    Frame tfr = null;
    Frame cvFoldAssignmentFrame = null;
    Frame foldId = null;
    GBMModel gbm = null;
    try {
      tfr = parse_test_file("smalldata/iris/iris_wheader.csv");
      foldId = new Frame(new String[]{"foldId"}, new Vec[]{AstKFold.kfoldColumn(tfr.vec("class").makeZero(), nfolds, 543216789)});
      tfr.add(foldId);
      DKV.put(tfr);

      GBMModel.GBMParameters parms = new GBMModel.GBMParameters();
      parms._train = tfr._key;
      parms._response_column = "class";
      parms._ntrees = 1;
      parms._max_depth = 1;
      parms._fold_column = "foldId";
      parms._distribution = DistributionFamily.multinomial;
      parms._keep_cross_validation_predictions=false;
      parms._keep_cross_validation_fold_assignment=true;
      GBM job = new GBM(parms);
      gbm = job.trainModel().get();

      assertNotNull(gbm._output._cross_validation_fold_assignment_frame_id);
      cvFoldAssignmentFrame = DKV.getGet(gbm._output._cross_validation_fold_assignment_frame_id);
      assertEquals(tfr.numRows(), cvFoldAssignmentFrame.numRows());
      isBitIdentical(foldId, cvFoldAssignmentFrame);

    } finally {
      if (tfr != null) tfr.remove();
      if (gbm != null) {
        gbm.delete();
        gbm.deleteCrossValidationModels();
      }
      if (cvFoldAssignmentFrame != null) cvFoldAssignmentFrame.delete();
    }
  }

  @Test public void checkFoldAssignmentsAreBeingRemovedAsSideEffectOfRemovingTrainingFrame() {
    final int nfolds = 3;
    Frame tfr = null;
    Frame cvFoldAssignmentFrame = null;
    Frame foldId = null;
    GBMModel gbm = null;
    try {
      tfr = parse_test_file("smalldata/iris/iris_wheader.csv");
      foldId = new Frame(new String[]{"foldId"}, new Vec[]{AstKFold.kfoldColumn(tfr.vec("class").makeZero(), nfolds, 543216789)});
      tfr.add(foldId);
      DKV.put(tfr);

      GBMModel.GBMParameters parms = new GBMModel.GBMParameters();
      parms._train = tfr._key;
      parms._response_column = "class";
      parms._ntrees = 1;
      parms._max_depth = 1;
      parms._fold_column = "foldId";
      parms._distribution = DistributionFamily.multinomial;
      parms._keep_cross_validation_predictions=false;
      parms._keep_cross_validation_fold_assignment=true;
      GBM job = new GBM(parms);
      gbm = job.trainModel().get();

      //Let's check that if we remove training frame we will also remove Vec for fold assignments.
      tfr.delete();
      assertNotNull(gbm._output._cross_validation_fold_assignment_frame_id);
      cvFoldAssignmentFrame = DKV.getGet(gbm._output._cross_validation_fold_assignment_frame_id);
      assertNull(DKV.get(cvFoldAssignmentFrame.vec("fold_assignment")._key));

    } finally {
      if (gbm != null) {
        gbm.delete();
        gbm.deleteCrossValidationModels();
      }
      if (cvFoldAssignmentFrame != null) cvFoldAssignmentFrame.delete();
    }
  }
}
