import chisel3._
import scala.math._

class conv_layer (MAX_MATRIX_WIDTH : Int,
                  MAX_FILTER_WIDTH : Int,
                  NUM_EXT_BIT      : Int,
                  WBIT_WIDTH       : Int,
                  DBIT_WIDTH       : Int) extends Module {
  val MAX_FILTER_SIZE : Int = MAX_FILTER_WIDTH * MAX_FILTER_WIDTH
  val DCTR_BIT_WIDTH  : Int = ceil(log(MAX_MATRIX_WIDTH)/log(2)).toInt
  val WCTR_BIT_WIDTH  : Int = ceil(log(MAX_FILTER_WIDTH)/log(2)).toInt
  val NUM_BIT_IN      : Int = ceil(log(WBIT_WIDTH)/log(2)).toInt

  val MAX : SInt = (pow(2, DBIT_WIDTH).toInt - 1).asSInt()
  val MIN : SInt = (-pow(2, DBIT_WIDTH).toInt).asSInt()

  val io = IO(new Bundle {
    val start_in   : Bool = Input(Bool())
    val data_ena   : Bool = Input(Bool())
    val data_in    : SInt = Input(SInt(DBIT_WIDTH.W))
//    val data_in    : UInt = Input(UInt(DBIT_WIDTH.W))
    val weight_ena : Bool = Input(Bool())
    val weight_in  : SInt = Input(SInt(WBIT_WIDTH.W))
    val bias_ena   : Bool = Input(Bool())
    val bias_in    : SInt = Input(SInt(WBIT_WIDTH.W))
    /*===============================================*/
    val prev_data_ena  : Bool = Input(Bool())
//    val prev_data_in   : UInt = Input(UInt(DBIT_WIDTH.W))
    val prev_data_in   : SInt = Input(SInt(DBIT_WIDTH.W))
    val stride_in      : UInt = Input(UInt(2.W))  // MAX_STRIDE = 4
    val filter_size_in : UInt = Input(UInt(3.W))  // MAX_FILTER_SIZE = 5
    val matrix_size_in : UInt = Input(UInt(5.W))  // MAX_MATRIX_SIZE = 32
    /*===============================================*/
    val data_done : Bool = Output(Bool())
    val data_out  : SInt = Output(SInt(DBIT_WIDTH.W))
  })

  val counter_width_reg  : UInt = RegInit(0.U(DCTR_BIT_WIDTH.W))
  val counter_height_reg : UInt = RegInit(0.U(DCTR_BIT_WIDTH.W))
  val stride_counter_reg : UInt = RegInit(0.U(DCTR_BIT_WIDTH.W))
  val stride_ena_reg     : Bool = RegInit(false.B)

  val weight_reg : Vec[SInt] = RegInit(VecInit(Seq.fill(MAX_FILTER_SIZE)(0.S(WBIT_WIDTH.W))))
  val data_reg   : Vec[SInt] = RegInit(VecInit(Seq.fill(MAX_FILTER_SIZE)(0.S(DBIT_WIDTH.W))))
//  val data_reg   : Vec[UInt] = RegInit(VecInit(Seq.fill(MAX_FILTER_SIZE)(0.U(DBIT_WIDTH.W))))

  val prev_data_reg : SInt = RegInit(0.S((DBIT_WIDTH+WBIT_WIDTH+NUM_EXT_BIT).W))
//  val mac_module : Array[vbp] = Array.fill(WBIT_WIDTH - 2) {
//    Module(new vbp(
//      NUM_BIT_IN,
//      WBIT_WIDTH,
//      DBIT_WIDTH,
//      MAX_FILTER_SIZE,
//      NUM_EXT_BIT
//    ))
//  }
//
//  val mac_ena_reg : Vec[UInt] = RegInit(VecInit(Seq.fill(WBIT_WIDTH - 2)(0.U(1.W))))

//  val mac_module : Array[vbp_uint] = Array.fill(DBIT_WIDTH - 2) {
//    Module(new vbp_uint(
//      NUM_BIT_IN,
//      WBIT_WIDTH,
//      DBIT_WIDTH,
//      MAX_FILTER_SIZE,
//      NUM_EXT_BIT
//    ))
//  }

  val mac_module : Array[vbp] = Array.fill(DBIT_WIDTH - 2) {
    Module(new vbp(
      NUM_BIT_IN,
      WBIT_WIDTH,
      DBIT_WIDTH,
      MAX_FILTER_SIZE,
      NUM_EXT_BIT
    ))
  }

  val mac_ena_reg : Vec[UInt] = RegInit(VecInit(Seq.fill(DBIT_WIDTH - 2)(0.U(1.W))))

  val mac_counter_reg : UInt = RegInit(0.U(NUM_BIT_IN.W))

  val stride_reg      : UInt = RegInit(0.U(2.W))
  val filter_size_reg : UInt = RegInit(0.U(3.W))
  val matrix_size_reg : UInt = RegInit(0.U(5.W))

  val data_out_reg : SInt = RegInit(0.S((DBIT_WIDTH+WBIT_WIDTH+NUM_EXT_BIT).W))

  val prev_data_ena_reg : Bool = RegInit(false.B)
  // ====================================================== //
  // =============== PARAMETER REGISTERS ================== //
  when (io.start_in) {
    stride_reg      := io.stride_in
    filter_size_reg := io.filter_size_in
    matrix_size_reg := io.matrix_size_in
  }

  // ====================================================== //
  // =============== VBP ================================== //
  // Need to consider that the number of width of filter is
  // smaller than the bit number of the filter's value. When
  // it happens, this module may not be working properly.
  for (i <- 0 until DBIT_WIDTH - 2) {
    mac_module(i).io.start_in <> mac_ena_reg(i)
    mac_module(i).io.a <> data_reg
    mac_module(i).io.w <> weight_reg
  }

//  io.data_done <> (0 until WBIT_WIDTH - 2).map(mac_module(_).io.done_out).reduce(_^_)
//  io.data_out  <> (0 until WBIT_WIDTH - 2).map(mac_module(_).io.sum).reduce(_+_)
  io.data_done <> (0 until DBIT_WIDTH - 2).map(mac_module(_).io.done_out).reduce(_^_)
  data_out_reg := (0 until DBIT_WIDTH - 2).map(mac_module(_).io.sum).reduce(_+_)

  prev_data_ena_reg := io.prev_data_ena

  when (prev_data_ena_reg) {
    when((data_out_reg >> WBIT_WIDTH).asSInt() + io.prev_data_in >= MAX) {
      io.data_out := MAX
    }.elsewhen((data_out_reg >> WBIT_WIDTH).asSInt() + io.prev_data_in < MIN) {
      io.data_out := MIN
    }.otherwise {
      io.data_out := (data_out_reg >> WBIT_WIDTH).asSInt() + io.prev_data_in
    }
  }.otherwise {
    when((data_out_reg >> WBIT_WIDTH).asSInt() >= MAX) {
      io.data_out := MAX
    }.elsewhen((data_out_reg >> WBIT_WIDTH).asSInt() < MIN) {
      io.data_out := MIN
    }.otherwise {
      io.data_out := (data_out_reg >> WBIT_WIDTH).asSInt()
    }
  }

  when (stride_ena_reg === true.B
    && counter_height_reg === filter_size_reg
    && stride_counter_reg === 0.U) {
//    when(mac_counter_reg === ((WBIT_WIDTH - 3).asUInt() - stride_reg)) {
    when(mac_counter_reg === ((DBIT_WIDTH - 3).asUInt() - stride_reg)) {
      mac_counter_reg := 0.U(NUM_BIT_IN.W)
    }.otherwise {
      mac_counter_reg := mac_counter_reg + 1.U
    }

//    mac_ena_reg := RegInit(VecInit(Seq.fill(WBIT_WIDTH - 2)(0.U(1.W))))
    mac_ena_reg := RegInit(VecInit(Seq.fill(DBIT_WIDTH - 2)(0.U(1.W))))
    mac_ena_reg(mac_counter_reg) := 1.U(1.W)
  }.elsewhen (io.start_in) {
//    mac_ena_reg     := RegInit(VecInit(Seq.fill(WBIT_WIDTH - 2)(0.U(1.W))))
    mac_ena_reg     := RegInit(VecInit(Seq.fill(DBIT_WIDTH - 2)(0.U(1.W))))
    mac_counter_reg := 0.U(NUM_BIT_IN.W)
  }.otherwise {
//    mac_ena_reg := RegInit(VecInit(Seq.fill(WBIT_WIDTH - 2)(0.U(1.W))))
    mac_ena_reg := RegInit(VecInit(Seq.fill(DBIT_WIDTH - 2)(0.U(1.W))))
  }

  // ====================================================== //
  // =============== STRIDE REGISTERS ===================== //
  // filter_size_reg = actual_filter_size - 1
  // stride_reg = actual_stride - 1
  when (stride_ena_reg) {
    when (counter_height_reg === filter_size_reg) {
      when (stride_counter_reg === stride_reg) {
        stride_counter_reg := 0.U(DCTR_BIT_WIDTH.W)
      }.otherwise {
        stride_counter_reg := stride_counter_reg + 1.U
      }
    }
  }.otherwise {
    stride_counter_reg := 0.U(DCTR_BIT_WIDTH.W)
  }

  when (io.data_ena) {
    when (counter_width_reg === filter_size_reg
      && counter_height_reg === filter_size_reg - 1.U) {
      stride_ena_reg := true.B
    }.elsewhen (counter_width_reg === matrix_size_reg
      && counter_height_reg === filter_size_reg) {
      stride_ena_reg := false.B
    }
  }

  // ====================================================== //
  // =============== DATA COUNTER REGISTER ================ //
  when (io.data_ena) {
    when (counter_height_reg === filter_size_reg) {
      counter_height_reg := 0.U(DCTR_BIT_WIDTH.W)
    }.otherwise {
      counter_height_reg := counter_height_reg + 1.U
    }

    // matrix_size_reg = actual_matrix_size - 1
    when (counter_height_reg === filter_size_reg) {
      when (counter_width_reg === matrix_size_reg) {
        counter_width_reg := 0.U(DCTR_BIT_WIDTH.W)
      }.otherwise {
        counter_width_reg := counter_width_reg + 1.U
      }
    }
  }.otherwise {
    counter_height_reg := 0.U(DCTR_BIT_WIDTH.W)
    counter_width_reg  := 0.U(DCTR_BIT_WIDTH.W)
  }

  // ====================================================== //
  // =============== DATA SHIFT REGISTER ================== //
  when (io.data_ena) {
    for (i <- 0 until MAX_FILTER_SIZE) {
      if (i == 0) {
        data_reg(i) := io.data_in
      } else {
        data_reg(i) := data_reg(i-1)
      }
    }
  }.elsewhen (io.start_in) {
//    data_reg := RegInit(VecInit(Seq.fill(MAX_FILTER_SIZE)(0.U(DBIT_WIDTH.W))))
    data_reg := RegInit(VecInit(Seq.fill(MAX_FILTER_SIZE)(0.S(DBIT_WIDTH.W))))
  }

  // ====================================================== //
  // =============== FILTER SHIFT REGISTER ================ //
  when (io.weight_ena) {
    for (i <- 0 until MAX_FILTER_SIZE) {
      if (i == 0) {
        weight_reg(i) := io.weight_in
      } else {
        weight_reg(i) := weight_reg(i-1)
      }
    }
  }.elsewhen (io.start_in) {
    weight_reg := RegInit(VecInit(Seq.fill(MAX_FILTER_SIZE)(0.S(DBIT_WIDTH.W))))
  }
}

