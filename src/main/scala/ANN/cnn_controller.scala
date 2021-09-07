import chisel3._
import chisel3.util._

class cnn_controller(ADDR_WIDTH : Int) extends Module {
  val io = IO(new Bundle {
    val start_in          : Bool = Input(Bool())
    // =============================================== //
    val conv_stride_in      : UInt = Input(UInt(2.W))
    val conv_filter_size_in : UInt = Input(UInt(3.W))
    val conv_matrix_size_in : UInt = Input(UInt(5.W))
    val conv_no_dim_in_in   : UInt = Input(UInt(3.W))
    val conv_no_dim_out_in  : UInt = Input(UInt(6.W))
    val pool_stride_in      : UInt = Input(UInt(2.W))
    val pool_filter_size_in : UInt = Input(UInt(3.W))
    val pool_matrix_size_in : UInt = Input(UInt(5.W))
    val fc_no_layer_in      : UInt = Input(UInt(6.W))
    val conv_no_layer_in    : UInt = Input(UInt(3.W))
    // =============================================== //
    val conv_stride_out      : UInt = Output(UInt(2.W))
    val conv_filter_size_out : UInt = Output(UInt(3.W))
    val conv_matrix_size_out : UInt = Output(UInt(5.W))
    val pool_stride_out      : UInt = Output(UInt(2.W))
    val pool_filter_size_out : UInt = Output(UInt(3.W))
    val pool_matrix_size_out : UInt = Output(UInt(5.W))
    // =============================================== //
    val conv_start_out : Bool = Output(Bool())
    val fc_start_out   : Bool = Output(Bool())
    val mem_select_out : Bool = Output(Bool())
    val load_mem_out   : Bool = Output(Bool())
    // =============================================== //
    val pool_done_in      : Bool = Input(Bool())
    val conv_data_done_in : Bool = Input(Bool())
    val pool_data_done_in : Bool = Input(Bool())
    val fc_done_in        : Bool = Input(Bool())
    val fc_data_done_in   : Bool = Input(Bool())
    val data_ready_in     : Bool = Input(Bool())
    val request_data_out  : Bool = Output(Bool())
    // =============================================== //
    val wr_data_in    : Bool = Input(Bool())
    val rd_data_0_out : Bool = Output(Bool())
    val rd_data_1_out : Bool = Output(Bool())
    val wr_data_0_out : Bool = Output(Bool())
    val wr_data_1_out : Bool = Output(Bool())
    val rd_weight_out : Bool = Output(Bool())
    val wr_weight_in  : Bool = Input(Bool())
    val rd_bias_out   : Bool = Output(Bool())
    val wr_bias_in    : Bool = Input(Bool())
    // =============================================== //
    val rd_data_addr_0_out : UInt = Output(UInt(ADDR_WIDTH.W))
    val rd_data_addr_1_out : UInt = Output(UInt(ADDR_WIDTH.W))
    val wr_data_addr_0_out : UInt = Output(UInt(ADDR_WIDTH.W))
    val wr_data_addr_1_out : UInt = Output(UInt(ADDR_WIDTH.W))
    val rd_weight_addr_out : UInt = Output(UInt(ADDR_WIDTH.W))
    val wr_weight_addr_out : UInt = Output(UInt(ADDR_WIDTH.W))
    val rd_bias_addr_out   : UInt = Output(UInt(ADDR_WIDTH.W))
    val wr_bias_addr_out   : UInt = Output(UInt(ADDR_WIDTH.W))
    // =============================================== //
    val done_out : Bool = Output(Bool())
  })
  val idle_state :: load_data_state :: conv_pool_state :: conv_switch_filter_state :: conv_switch_layer_state :: fully_connected_state :: fc_switch_layer_state :: done_state :: Nil = Enum(8)

  val state_reg = RegInit(idle_state)

  val conv_no_dim_out_reg      : UInt = RegInit(0.U(6.W))
  val conv_dim_out_counter_reg : UInt = RegInit(0.U(6.W))
  val conv_no_dim_in_reg       : UInt = RegInit(0.U(3.W))
  val conv_dim_in_counter_reg  : UInt = RegInit(0.U(3.W))
  val conv_no_layer_reg        : UInt = RegInit(0.U(3.W))
  val conv_layer_counter_reg   : UInt = RegInit(0.U(3.W))
  val fc_no_layer_reg          : UInt = RegInit(0.U(3.W))
  val fc_layer_counter_reg     : UInt = RegInit(0.U(3.W))

  val conv_stride_reg      : UInt = RegInit(0.U(2.W))
  val conv_filter_size_reg : UInt = RegInit(0.U(3.W))
  val conv_matrix_size_reg : UInt = RegInit(0.U(5.W))

  val pool_stride_reg      : UInt = RegInit(0.U(2.W))
  val pool_filter_size_reg : UInt = RegInit(0.U(3.W))
  val pool_matrix_size_reg : UInt = RegInit(0.U(5.W))

  val rd_data_0_reg : Bool = RegInit(false.B)
  val rd_data_1_reg : Bool = RegInit(false.B)

  val rd_data_0_w : Bool = WireInit(false.B)
  val rd_data_1_w : Bool = WireInit(false.B)
  val wr_data_0_w : Bool = WireInit(false.B)
  val wr_data_1_w : Bool = WireInit(false.B)
  val rd_weight_w : Bool = WireInit(false.B)
  val wr_weight_w : Bool = WireInit(false.B)
  val rd_bias_w   : Bool = WireInit(false.B)
  val wr_bias_w   : Bool = WireInit(false.B)

  val clear_para_addr_w : Bool = WireInit(false.B)
  val clear_data_addr_w : Bool = WireInit(false.B)

  val request_data_reg  : Bool = RegInit(false.B)
  val mem_select_reg    : Bool = RegInit(false.B)

  val load_mem_w   : Bool = WireInit(false.B)
  val conv_start_w : Bool = WireInit(false.B)
  val fc_start_w   : Bool = WireInit(false.B)
  val done_w       : Bool = WireInit(false.B)

  val rd_data_addr_0_reg : UInt = RegInit(0.U(ADDR_WIDTH.W))
  val rd_data_addr_1_reg : UInt = RegInit(0.U(ADDR_WIDTH.W))
  val wr_data_addr_0_reg : UInt = RegInit(0.U(ADDR_WIDTH.W))
  val wr_data_addr_1_reg : UInt = RegInit(0.U(ADDR_WIDTH.W))
  val rd_weight_addr_reg : UInt = RegInit(0.U(ADDR_WIDTH.W))
  val wr_weight_addr_reg : UInt = RegInit(0.U(ADDR_WIDTH.W))
  val rd_bias_addr_reg   : UInt = RegInit(0.U(ADDR_WIDTH.W))
  val wr_bias_addr_reg   : UInt = RegInit(0.U(ADDR_WIDTH.W))

  val data_base_addr_reg : UInt = RegInit(0.U(ADDR_WIDTH.W))

  val weight_counter_width_reg  : UInt = RegInit(0.U(3.W))
  val weight_counter_height_reg : UInt = RegInit(0.U(3.W))

  val data_counter_width_reg  : UInt = RegInit(0.U(5.W))
  val data_counter_height_reg : UInt = RegInit(0.U(5.W))
  val data_filter_counter_reg : UInt = RegInit(0.U(3.W))

  // =================== FINITE STATE MACHINE ================= //
  switch (state_reg) {
    //================== IDLE STATE ====================//
    is (idle_state) {
      when (io.start_in) {
        state_reg := load_data_state
      }.otherwise {
        state_reg := idle_state
      }
    }
    //================== LOAD DATA STATE ===============//
    is (load_data_state) {
      when (io.data_ready_in) {
        state_reg := conv_pool_state
      }.otherwise {
        state_reg := load_data_state
      }
    }
    //================== CONVOLUTION + POOLING STATE ===//
    is (conv_pool_state) {
      when (io.pool_done_in) {
        state_reg := conv_switch_filter_state
      }.otherwise {
        state_reg := conv_pool_state
      }
    }
    //================== SWITCHING CONV FILTER STATE ===//
    is (conv_switch_filter_state) {
      when (conv_dim_out_counter_reg === conv_no_dim_out_reg
        && conv_dim_in_counter_reg === conv_no_dim_in_reg) {
        state_reg := conv_switch_layer_state
      }.otherwise {
        state_reg := conv_pool_state
      }
    }
    //================== SWITCHING CONV LAYER STATE ====//
    is (conv_switch_layer_state) {
      when (conv_layer_counter_reg === conv_no_layer_reg) {
        state_reg := fully_connected_state
      }.otherwise {
        state_reg := conv_pool_state
      }
    }
    //================== FULLY CONNECTED STATE =========//
    is (fully_connected_state) {
      when (io.fc_done_in) {
        state_reg := fc_switch_layer_state
      }.otherwise {
        state_reg := fully_connected_state
      }
    }
    //================== SWITCHING FC LAYER STATE ======//
    is (fc_switch_layer_state) {
      when (fc_layer_counter_reg === fc_no_layer_reg) {
        state_reg := done_state
      }.otherwise {
        state_reg := fully_connected_state
      }
    }
    //================== DONE STATE ====================//
    is (done_state) {
      state_reg := idle_state
    }
  }

  // =========================================================== //
  // =================== CONTROL PROCESSING ==================== //
  when (io.start_in) {
    conv_stride_reg      := io.conv_stride_in
    conv_filter_size_reg := io.conv_filter_size_in
    conv_matrix_size_reg := io.conv_matrix_size_in

    pool_stride_reg      := io.pool_stride_in
    pool_filter_size_reg := io.pool_filter_size_in
    pool_matrix_size_reg := io.pool_matrix_size_in

    conv_no_dim_out_reg := io.conv_no_dim_out_in
    conv_no_dim_in_reg  := io.conv_no_dim_in_in
    conv_no_layer_reg   := io.conv_no_layer_in
    fc_no_layer_reg     := io.fc_no_layer_in
  }

  when (conv_start_w) {
    io.conv_stride_out      := conv_stride_reg
    io.conv_filter_size_out := conv_filter_size_reg
    io.conv_matrix_size_out := conv_matrix_size_reg

    io.pool_stride_out      := pool_stride_reg
    io.pool_filter_size_out := pool_filter_size_reg
    io.pool_matrix_size_out := pool_matrix_size_reg
  }.otherwise {
    io.conv_stride_out      := 0.U
    io.conv_filter_size_out := 0.U
    io.conv_matrix_size_out := 0.U

    io.pool_stride_out      := 0.U
    io.pool_filter_size_out := 0.U
    io.pool_matrix_size_out := 0.U
  }

  when (state_reg === idle_state) {
    conv_dim_out_counter_reg := 0.U(3.W)
    conv_dim_in_counter_reg  := 0.U(6.W)
    conv_layer_counter_reg   := 0.U(3.W)
    fc_layer_counter_reg     := 0.U(3.W)
  }.otherwise {
    when (state_reg === conv_switch_filter_state) {
      when (conv_dim_in_counter_reg === conv_no_dim_in_reg) {
        conv_dim_in_counter_reg := 0.U(6.W)
        when (conv_dim_out_counter_reg === conv_no_dim_out_reg) {
          conv_dim_out_counter_reg := 0.U(3.W)
        }.otherwise {
          conv_dim_out_counter_reg := conv_dim_out_counter_reg + 1.U
        }
      }.otherwise {
        conv_dim_in_counter_reg := conv_dim_in_counter_reg + 1.U
      }
    }
    when (state_reg === conv_switch_layer_state) {
      conv_layer_counter_reg := conv_layer_counter_reg + 1.U
    }
    when (state_reg === fc_switch_layer_state) {
      fc_layer_counter_reg := fc_layer_counter_reg + 1.U
    }
  }

  when (state_reg === idle_state) {
    // READ ENABLE SIGNALS OF DATA MEMORY //
    rd_data_0_w  := false.B
    rd_data_1_w  := false.B
    // WRITE ENABLE SIGNALS OF DATA MEMORY //
    wr_data_0_w  := false.B
    wr_data_1_w  := false.B
    // READ ENABLE SIGNALS OF WEIGHT MEMORY //
    rd_weight_w  := false.B
    rd_bias_w    := false.B

    clear_data_addr_w := false.B
    clear_para_addr_w := false.B

    load_mem_w   := false.B
    conv_start_w := false.B
    fc_start_w   := false.B
    done_w       := false.B
  }.elsewhen (state_reg === load_data_state) {
    // READ ENABLE SIGNALS OF DATA MEMORY //
    rd_data_0_w  := false.B
    rd_data_1_w  := false.B
    // WRITE ENABLE SIGNALS OF DATA MEMORY //
    wr_data_0_w  := false.B
    wr_data_1_w  := false.B
    // READ ENABLE SIGNALS OF WEIGHT MEMORY //
    rd_weight_w  := false.B
    rd_bias_w    := false.B

    clear_para_addr_w := false.B

    when (io.data_ready_in) {
      conv_start_w      := true.B
      clear_data_addr_w := true.B
    }.otherwise {
      conv_start_w      := false.B
      clear_data_addr_w := false.B
    }
    load_mem_w   := true.B
    fc_start_w   := false.B
    done_w       := false.B
  }.elsewhen (state_reg === conv_pool_state) {
    // READ ENABLE SIGNALS OF DATA MEMORY //
    when (mem_select_reg) {
      when(data_counter_height_reg === conv_matrix_size_reg - conv_filter_size_reg + 1.U) {
        rd_data_0_w := false.B
      }.otherwise {
        rd_data_0_w := true.B
      }
      rd_data_1_w := io.conv_data_done_in
    }.otherwise {
      when(data_counter_height_reg === conv_matrix_size_reg - conv_filter_size_reg + 1.U) {
        rd_data_1_w := false.B
      }.otherwise {
        rd_data_1_w := true.B
      }
      rd_data_0_w := io.conv_data_done_in
    }
    // WRITE ENABLE SIGNALS OF DATA MEMORY //
    when (mem_select_reg) {
      when(io.pool_data_done_in) {
        wr_data_0_w := true.B
      }.otherwise {
        wr_data_0_w := false.B
      }
      when(io.conv_data_done_in) {
        wr_data_1_w := true.B
      }.otherwise {
        wr_data_1_w := false.B
      }
    }.otherwise {
      when(io.pool_data_done_in) {
        wr_data_1_w := true.B
      }.otherwise {
        wr_data_1_w := false.B
      }
      when(io.conv_data_done_in) {
        wr_data_0_w := true.B
      }.otherwise {
        wr_data_0_w := false.B
      }
    }
    // READ ENABLE SIGNALS OF WEIGHT MEMORY //
    when(weight_counter_height_reg === conv_filter_size_reg + 1.U) {
      rd_weight_w := false.B
    }.otherwise {
      rd_weight_w := true.B
    }
    rd_bias_w := true.B

    clear_data_addr_w := false.B
    clear_para_addr_w := false.B

    load_mem_w   := false.B
    conv_start_w := false.B
    fc_start_w   := false.B
    done_w       := false.B
  }.elsewhen (state_reg === conv_switch_filter_state) {
    // READ ENABLE SIGNALS OF DATA MEMORY //
    rd_data_0_w  := false.B
    rd_data_1_w  := false.B
    // WRITE ENABLE SIGNALS OF DATA MEMORY //
    wr_data_0_w  := false.B
    wr_data_1_w  := false.B
    // READ ENABLE SIGNALS OF WEIGHT MEMORY //
    rd_weight_w  := false.B
    rd_bias_w    := false.B

    clear_data_addr_w := true.B
    clear_para_addr_w := false.B

    when (conv_dim_in_counter_reg === conv_no_dim_in_reg
      && conv_dim_out_counter_reg === conv_no_dim_out_reg) {
      conv_start_w := false.B
      done_w       := true.B
    }.otherwise {
      conv_start_w := true.B
      done_w       := false.B
    }
    load_mem_w  := false.B
    fc_start_w  := false.B
  }.elsewhen (state_reg === conv_switch_layer_state) {
    // READ ENABLE SIGNALS OF DATA MEMORY //
    rd_data_0_w := false.B
    rd_data_1_w := false.B
    // WRITE ENABLE SIGNALS OF DATA MEMORY //
    wr_data_0_w := false.B
    wr_data_1_w := false.B
    // READ ENABLE SIGNALS OF WEIGHT MEMORY //
    rd_weight_w := false.B
    rd_bias_w   := false.B

    clear_data_addr_w := true.B
    clear_para_addr_w := false.B

    when(conv_dim_in_counter_reg === conv_no_dim_in_reg) {
      conv_start_w := false.B
    }.otherwise {
      conv_start_w := true.B
    }
    fc_start_w := false.B
    load_mem_w := false.B
    done_w     := false.B
  }.elsewhen (state_reg === fully_connected_state) {
    // READ ENABLE SIGNALS OF DATA MEMORY //
    when (mem_select_reg) {
      when(data_counter_height_reg === conv_matrix_size_reg) {
        rd_data_0_w := false.B
      }.otherwise {
        rd_data_0_w := true.B
      }
      rd_data_1_w := io.fc_data_done_in
    }.otherwise {
      when(data_counter_height_reg === conv_matrix_size_reg) {
        rd_data_1_w := false.B
      }.otherwise {
        rd_data_1_w := true.B
      }
      rd_data_0_w := io.fc_data_done_in
    }
    // WRITE ENABLE SIGNALS OF DATA MEMORY //
    wr_data_0_w := rd_data_0_reg
    wr_data_1_w := rd_data_1_reg
    // READ ENABLE SIGNALS OF WEIGHT MEMORY //
    rd_weight_w := false.B
    rd_bias_w   := false.B

    clear_data_addr_w := false.B
    clear_para_addr_w := false.B

    load_mem_w   := false.B
    conv_start_w := false.B
    fc_start_w   := false.B
    done_w       := false.B
  }.elsewhen (state_reg === fc_switch_layer_state) {
    // READ ENABLE SIGNALS OF DATA MEMORY //
    rd_data_0_w := false.B
    rd_data_1_w := false.B
    // WRITE ENABLE SIGNALS OF DATA MEMORY //
    wr_data_0_w := false.B
    wr_data_1_w := false.B
    // READ ENABLE SIGNALS OF WEIGHT MEMORY //
    rd_weight_w := false.B
    rd_bias_w   := false.B

    clear_data_addr_w := false.B
    clear_para_addr_w := false.B

    load_mem_w   := false.B
    conv_start_w := false.B
    when(fc_layer_counter_reg === fc_no_layer_reg) {
      fc_start_w := false.B
    }.otherwise {
      fc_start_w := true.B
    }
    done_w       := false.B
  }.elsewhen (state_reg === done_state) {
    // READ ENABLE SIGNALS OF DATA MEMORY //
    rd_data_0_w := false.B
    rd_data_1_w := false.B
    // WRITE ENABLE SIGNALS OF DATA MEMORY //
    wr_data_0_w := false.B
    wr_data_1_w := false.B
    // READ ENABLE SIGNALS OF WEIGHT MEMORY //
    rd_weight_w := false.B
    rd_bias_w   := false.B

    clear_data_addr_w := true.B
    clear_para_addr_w := true.B

    load_mem_w   := false.B
    conv_start_w := false.B
    fc_start_w   := false.B
    done_w       := true.B
  }.otherwise {
    // READ ENABLE SIGNALS OF DATA MEMORY //
    rd_data_0_w := false.B
    rd_data_1_w := false.B
    // WRITE ENABLE SIGNALS OF DATA MEMORY //
    wr_data_0_w := false.B
    wr_data_1_w := false.B
    // READ ENABLE SIGNALS OF WEIGHT MEMORY //
    rd_weight_w := false.B
    rd_bias_w   := false.B

    clear_data_addr_w := false.B
    clear_para_addr_w := false.B

    load_mem_w   := false.B
    conv_start_w := false.B
    fc_start_w   := false.B
    done_w       := false.B
  }

  rd_data_0_reg := rd_data_0_w
  rd_data_1_reg := rd_data_1_w

  wr_weight_w := io.wr_weight_in
  wr_bias_w   := io.wr_bias_in
  when (mem_select_reg) {
    io.rd_data_0_out := rd_data_0_w
    when (conv_dim_in_counter_reg =/= 0.U) {
      io.rd_data_1_out := rd_data_1_w
    }.otherwise {
      io.rd_data_1_out := false.B
    }
  }.otherwise {
    io.rd_data_1_out := rd_data_1_w
    when (conv_dim_in_counter_reg =/= 0.U) {
      io.rd_data_0_out := rd_data_0_w
    }.otherwise {
      io.rd_data_0_out := false.B
    }
  }
  when (io.wr_data_in === true.B || wr_data_0_w === true.B) {
    io.wr_data_0_out := true.B
  }.otherwise {
    io.wr_data_0_out := false.B
  }
  io.wr_data_1_out := wr_data_1_w

  io.rd_weight_out := rd_weight_w
  io.rd_bias_out   := rd_bias_w

  io.conv_start_out := conv_start_w
  io.fc_start_out   := fc_start_w
  io.done_out       := done_w

  request_data_reg    := conv_start_w ^ fc_start_w
  io.request_data_out := request_data_reg
  io.mem_select_out   := mem_select_reg
  io.load_mem_out     := load_mem_w

  when ((state_reg === conv_switch_layer_state) || io.data_ready_in) {
    mem_select_reg := ~mem_select_reg
  }

  // =========================================================== //
  // =================== ADDRESS PROCESSING ==================== //
  when (state_reg === idle_state) {
    // READ ADDRESS OF DATA MEMORY //
    rd_data_addr_0_reg := 0.U(ADDR_WIDTH.W)
    rd_data_addr_1_reg := 0.U(ADDR_WIDTH.W)
    // WRITE ADDRESS OF DATA MEMORY //
    wr_data_addr_0_reg := 0.U(ADDR_WIDTH.W)
    wr_data_addr_1_reg := 0.U(ADDR_WIDTH.W)
    // READ/WRITE ADDRESS OF WEIGHT MEMORY //
    rd_weight_addr_reg := 0.U(ADDR_WIDTH.W)
    wr_weight_addr_reg := 0.U(ADDR_WIDTH.W)
    rd_bias_addr_reg   := 0.U(ADDR_WIDTH.W)
    wr_bias_addr_reg   := 0.U(ADDR_WIDTH.W)

    data_base_addr_reg := 0.U(ADDR_WIDTH.W)
  }.elsewhen (io.data_ready_in) {
    // READ ADDRESS OF DATA MEMORY //
    rd_data_addr_0_reg := 0.U(ADDR_WIDTH.W)
    rd_data_addr_1_reg := 0.U(ADDR_WIDTH.W)
    // WRITE ADDRESS OF DATA MEMORY //
    wr_data_addr_0_reg := 0.U(ADDR_WIDTH.W)
    wr_data_addr_1_reg := 0.U(ADDR_WIDTH.W)

    data_base_addr_reg := 0.U(ADDR_WIDTH.W)
  }.elsewhen (clear_data_addr_w) {
    // READ ADDRESS OF DATA MEMORY //
    when (conv_dim_in_counter_reg === conv_no_dim_in_reg) {
      data_base_addr_reg := 0.U(ADDR_WIDTH.W)
      rd_data_addr_0_reg := 0.U(ADDR_WIDTH.W)
      rd_data_addr_1_reg := 0.U(ADDR_WIDTH.W)
      // WRITE ADDRESS OF DATA MEMORY //
      when (conv_dim_out_counter_reg === conv_no_dim_out_reg) {
        wr_data_addr_0_reg := 0.U(ADDR_WIDTH.W)
        wr_data_addr_1_reg := 0.U(ADDR_WIDTH.W)
      }
    }
  }.elsewhen (clear_para_addr_w) {
    // READ/WRITE ADDRESS OF WEIGHT MEMORY //
    rd_weight_addr_reg := 0.U(ADDR_WIDTH.W)
    wr_weight_addr_reg := 0.U(ADDR_WIDTH.W)
    rd_bias_addr_reg   := 0.U(ADDR_WIDTH.W)
    wr_bias_addr_reg   := 0.U(ADDR_WIDTH.W)
  }.otherwise {
    // READ ADDRESS OF DATA MEMORY //
    when (mem_select_reg) {
      when(rd_data_0_w) {
        when(data_filter_counter_reg === conv_filter_size_reg) {
          when(data_counter_width_reg === conv_matrix_size_reg
            && data_counter_height_reg === conv_matrix_size_reg - conv_filter_size_reg) {
            rd_data_addr_0_reg := rd_data_addr_0_reg + 1.U
            data_base_addr_reg := rd_data_addr_0_reg + 1.U
          }.otherwise {
            rd_data_addr_0_reg := data_base_addr_reg + 1.U
            data_base_addr_reg := data_base_addr_reg + 1.U
          }
        }.otherwise {
          rd_data_addr_0_reg := rd_data_addr_0_reg + conv_matrix_size_reg + 1.U
        }
      }
      when(rd_data_1_w) {
        rd_data_addr_1_reg := rd_data_addr_1_reg + 1.U
      }
    }.otherwise {
      when(rd_data_1_w) {
        when(data_filter_counter_reg === conv_filter_size_reg) {
          when(data_counter_width_reg === conv_matrix_size_reg
            && data_counter_height_reg === conv_matrix_size_reg - conv_filter_size_reg) {
            rd_data_addr_1_reg := rd_data_addr_1_reg + 1.U
            data_base_addr_reg := rd_data_addr_1_reg + 1.U
          }.otherwise {
            rd_data_addr_1_reg := data_base_addr_reg + 1.U
            data_base_addr_reg := data_base_addr_reg + 1.U
          }
        }.otherwise {
          rd_data_addr_1_reg := rd_data_addr_1_reg + conv_matrix_size_reg + 1.U
        }
      }
      when(rd_data_0_w) {
        rd_data_addr_0_reg := rd_data_addr_0_reg + 1.U
      }
    }
    // WRITE ADDRESS OF DATA MEMORY //
    when (wr_data_0_w ^ io.wr_data_in) {
      wr_data_addr_0_reg := wr_data_addr_0_reg + 1.U
    }
    when (wr_data_1_w) {
      wr_data_addr_1_reg := wr_data_addr_1_reg + 1.U
    }
    // READ/WRITE ADDRESS OF WEIGHT MEMORY //
    when (rd_weight_w) {
      rd_weight_addr_reg := rd_weight_addr_reg + 1.U
    }
    when (wr_weight_w) {
      wr_weight_addr_reg := wr_weight_addr_reg + 1.U
    }
    when (rd_bias_w) {
      rd_bias_addr_reg   := rd_bias_addr_reg + 1.U
    }
    when (wr_bias_w) {
      wr_bias_addr_reg   := wr_bias_addr_reg + 1.U
    }
  }

  io.rd_data_addr_0_out := rd_data_addr_0_reg
  io.rd_data_addr_1_out := rd_data_addr_1_reg
  io.wr_data_addr_0_out := wr_data_addr_0_reg
  io.wr_data_addr_1_out := wr_data_addr_1_reg
  io.rd_weight_addr_out := rd_weight_addr_reg
  io.wr_weight_addr_out := wr_weight_addr_reg
  io.rd_bias_addr_out   := rd_bias_addr_reg
  io.wr_bias_addr_out   := wr_bias_addr_reg

  // =========================================================== //
  // =================== COUNTER PROCESSING ==================== //
  when (rd_weight_w) {
    when (weight_counter_width_reg === conv_filter_size_reg) {
      weight_counter_width_reg  := 0.U
      weight_counter_height_reg := weight_counter_height_reg + 1.U
    }.otherwise {
      weight_counter_width_reg := weight_counter_width_reg + 1.U
    }
  }.elsewhen (io.pool_done_in) {
    weight_counter_width_reg  := 0.U
    weight_counter_height_reg := 0.U
  }

  when (mem_select_reg) {
    when(rd_data_0_w) {
      when(data_filter_counter_reg === conv_filter_size_reg) {
        data_filter_counter_reg := 0.U
      }.otherwise {
        data_filter_counter_reg := data_filter_counter_reg + 1.U
      }

      when(data_counter_width_reg === conv_matrix_size_reg
        && data_filter_counter_reg === conv_filter_size_reg) {
        data_counter_width_reg  := 0.U
        data_counter_height_reg := data_counter_height_reg + 1.U
      }.elsewhen(data_filter_counter_reg === conv_filter_size_reg) {
        data_counter_width_reg  := data_counter_width_reg + 1.U
      }
    }.elsewhen(io.pool_done_in) {
      data_counter_width_reg  := 0.U
      data_counter_height_reg := 0.U
      data_filter_counter_reg := 0.U
    }
  }.otherwise {
    when(rd_data_1_w) {
      when(data_filter_counter_reg === conv_filter_size_reg) {
        data_filter_counter_reg := 0.U
      }.otherwise {
        data_filter_counter_reg := data_filter_counter_reg + 1.U
      }

      when(data_counter_width_reg === conv_matrix_size_reg
        && data_filter_counter_reg === conv_filter_size_reg) {
        data_counter_width_reg  := 0.U
        data_counter_height_reg := data_counter_height_reg + 1.U
      }.elsewhen(data_filter_counter_reg === conv_filter_size_reg) {
        data_counter_width_reg  := data_counter_width_reg + 1.U
      }
    }.elsewhen(io.pool_done_in) {
      data_counter_width_reg  := 0.U
      data_counter_height_reg := 0.U
      data_filter_counter_reg := 0.U
    }
  }

  // =========================================================== //
}
