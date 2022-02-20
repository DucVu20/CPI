module SinglePortRam(
  input         clock,
  input  [16:0] io_addr,
  input  [15:0] io_dataIn,
  output [15:0] io_dataOut,
  input         io_wrEna,
  input         io_rdEna
);
`ifdef RANDOMIZE_GARBAGE_ASSIGN
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_GARBAGE_ASSIGN
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
`endif // RANDOMIZE_REG_INIT
  reg [15:0] mem [0:102079]; // @[SinglePortedRam.scala 16:24]
  wire [15:0] mem_io_dataOut_MPORT_data; // @[SinglePortedRam.scala 16:24]
  wire [16:0] mem_io_dataOut_MPORT_addr; // @[SinglePortedRam.scala 16:24]
  wire [15:0] mem_MPORT_data; // @[SinglePortedRam.scala 16:24]
  wire [16:0] mem_MPORT_addr; // @[SinglePortedRam.scala 16:24]
  wire  mem_MPORT_mask; // @[SinglePortedRam.scala 16:24]
  wire  mem_MPORT_en; // @[SinglePortedRam.scala 16:24]
  reg  mem_io_dataOut_MPORT_en_pipe_0;
  reg [16:0] mem_io_dataOut_MPORT_addr_pipe_0;
  assign mem_io_dataOut_MPORT_addr = mem_io_dataOut_MPORT_addr_pipe_0;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign mem_io_dataOut_MPORT_data = mem[mem_io_dataOut_MPORT_addr]; // @[SinglePortedRam.scala 16:24]
  `else
  assign mem_io_dataOut_MPORT_data = mem_io_dataOut_MPORT_addr >= 17'h18ec0 ? _RAND_1[15:0] :
    mem[mem_io_dataOut_MPORT_addr]; // @[SinglePortedRam.scala 16:24]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign mem_MPORT_data = io_dataIn;
  assign mem_MPORT_addr = io_addr;
  assign mem_MPORT_mask = 1'h1;
  assign mem_MPORT_en = io_wrEna;
  assign io_dataOut = mem_io_dataOut_MPORT_data; // @[SinglePortedRam.scala 20:14]
  always @(posedge clock) begin
    if(mem_MPORT_en & mem_MPORT_mask) begin
      mem[mem_MPORT_addr] <= mem_MPORT_data; // @[SinglePortedRam.scala 16:24]
    end
    mem_io_dataOut_MPORT_en_pipe_0 <= io_rdEna;
    if (io_rdEna) begin
      mem_io_dataOut_MPORT_addr_pipe_0 <= io_addr;
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_GARBAGE_ASSIGN
  _RAND_1 = {1{`RANDOM}};
`endif // RANDOMIZE_GARBAGE_ASSIGN
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 102080; initvar = initvar+1)
    mem[initvar] = _RAND_0[15:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_2 = {1{`RANDOM}};
  mem_io_dataOut_MPORT_en_pipe_0 = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  mem_io_dataOut_MPORT_addr_pipe_0 = _RAND_3[16:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module CaptureModule(
  input         clock,
  input         reset,
  input         io_pclk,
  input         io_href,
  input         io_vsync,
  input  [7:0]  io_pixelIn,
  output [15:0] io_pixelOut,
  output [16:0] io_pixelAddr,
  output [9:0]  io_frameWidth,
  output [8:0]  io_frameHeight,
  input         io_capture,
  input         io_videoMode,
  output        io_capturing,
  input         io_readFrame,
  output        io_pixelValid,
  output        io_frameFull,
  output        io_newFrame
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_11;
  reg [31:0] _RAND_12;
  reg [31:0] _RAND_13;
  reg [31:0] _RAND_14;
  reg [31:0] _RAND_15;
  reg [31:0] _RAND_16;
  reg [31:0] _RAND_17;
  reg [31:0] _RAND_18;
  reg [31:0] _RAND_19;
`endif // RANDOMIZE_REG_INIT
  wire  buffer_clock; // @[CaptureModule.scala 78:22]
  wire [16:0] buffer_io_addr; // @[CaptureModule.scala 78:22]
  wire [15:0] buffer_io_dataIn; // @[CaptureModule.scala 78:22]
  wire [15:0] buffer_io_dataOut; // @[CaptureModule.scala 78:22]
  wire  buffer_io_wrEna; // @[CaptureModule.scala 78:22]
  wire  buffer_io_rdEna; // @[CaptureModule.scala 78:22]
  reg  FMS; // @[CaptureModule.scala 52:20]
  reg [16:0] writePtr; // @[CaptureModule.scala 54:27]
  reg [16:0] readPtr; // @[CaptureModule.scala 55:27]
  reg [7:0] firstByte; // @[CaptureModule.scala 56:27]
  reg [7:0] secondByte; // @[CaptureModule.scala 57:27]
  reg  captureSignalHolder; // @[CaptureModule.scala 62:36]
  reg [16:0] bufferDepthCounter; // @[CaptureModule.scala 64:36]
  reg  newFrame; // @[CaptureModule.scala 65:36]
  reg  pixelIndex; // @[CaptureModule.scala 66:36]
  reg [8:0] rowCnt; // @[CaptureModule.scala 67:36]
  reg [9:0] colCnt; // @[CaptureModule.scala 68:36]
  reg  frameFull; // @[CaptureModule.scala 69:36]
  reg  wrEna; // @[CaptureModule.scala 75:28]
  reg  pixelValid; // @[CaptureModule.scala 76:28]
  reg  vsyncFallingEdge_REG; // @[CaptureModule.scala 80:50]
  wire  vsyncFallingEdge = ~io_vsync & vsyncFallingEdge_REG; // @[CaptureModule.scala 80:38]
  wire  _hrefFallingEdge_T = ~io_href; // @[CaptureModule.scala 81:27]
  reg  hrefRisingEdge_REG; // @[CaptureModule.scala 82:50]
  wire  hrefRisingEdge = io_href & ~hrefRisingEdge_REG; // @[CaptureModule.scala 82:38]
  reg  pclkRisingEdge_REG; // @[CaptureModule.scala 83:50]
  wire  pclkRisingEdge = io_pclk & ~pclkRisingEdge_REG; // @[CaptureModule.scala 83:38]
  wire  _T_6 = ~FMS; // @[Conditional.scala 37:30]
  wire  capturing = _T_6 ? 1'h0 : FMS; // @[Conditional.scala 40:58 CaptureModule.scala 126:17]
  wire [16:0] _readPtr_T_1 = readPtr + 17'h1; // @[CaptureModule.scala 89:29]
  wire  _T_4 = ~capturing; // @[CaptureModule.scala 91:17]
  wire [16:0] _GEN_0 = ~capturing ? readPtr : 17'h0; // @[CaptureModule.scala 91:28 CaptureModule.scala 92:18]
  wire [16:0] _GEN_1 = ~capturing ? _readPtr_T_1 : readPtr; // @[CaptureModule.scala 91:28 CaptureModule.scala 93:18 CaptureModule.scala 55:27]
  wire [16:0] _GEN_3 = capturing & _hrefFallingEdge_T ? readPtr : _GEN_0; // @[CaptureModule.scala 87:34 CaptureModule.scala 88:18]
  wire  _GEN_5 = capturing & _hrefFallingEdge_T | _T_4; // @[CaptureModule.scala 87:34 CaptureModule.scala 90:18]
  wire  _GEN_8 = readPtr == bufferDepthCounter ? 1'h0 : frameFull; // @[CaptureModule.scala 98:42 CaptureModule.scala 101:26 CaptureModule.scala 69:36]
  reg [16:0] bufferAddr_REG; // @[CaptureModule.scala 105:26]
  wire  _GEN_14 = (io_videoMode | io_readFrame) & frameFull ? _GEN_8 : frameFull; // @[CaptureModule.scala 86:55 CaptureModule.scala 69:36]
  wire  _captureSignalHolder_T = io_capture ? io_capture : captureSignalHolder; // @[CaptureModule.scala 108:30]
  wire  _GEN_15 = vsyncFallingEdge | FMS; // @[CaptureModule.scala 116:34 CaptureModule.scala 117:33 CaptureModule.scala 52:20]
  wire  _GEN_16 = vsyncFallingEdge ? 1'h0 : _captureSignalHolder_T; // @[CaptureModule.scala 116:34 CaptureModule.scala 118:33 CaptureModule.scala 108:24]
  wire  _GEN_17 = vsyncFallingEdge ? 1'h0 : newFrame; // @[CaptureModule.scala 116:34 CaptureModule.scala 119:33 CaptureModule.scala 65:36]
  wire [8:0] _GEN_18 = vsyncFallingEdge ? 9'h0 : rowCnt; // @[CaptureModule.scala 116:34 CaptureModule.scala 120:33 CaptureModule.scala 67:36]
  wire [9:0] _GEN_19 = vsyncFallingEdge ? 10'h0 : colCnt; // @[CaptureModule.scala 116:34 CaptureModule.scala 121:33 CaptureModule.scala 68:36]
  wire [16:0] _GEN_20 = vsyncFallingEdge ? 17'h0 : writePtr; // @[CaptureModule.scala 116:34 CaptureModule.scala 122:33 CaptureModule.scala 54:27]
  wire  _pixelIndex_T = ~pixelIndex; // @[CaptureModule.scala 135:25]
  wire [7:0] _GEN_33 = pixelIndex ? io_pixelIn : secondByte; // @[Conditional.scala 39:67 CaptureModule.scala 141:26 CaptureModule.scala 57:27]
  wire  _GEN_34 = pixelIndex & pclkRisingEdge; // @[Conditional.scala 39:67 CaptureModule.scala 142:25]
  wire [7:0] _GEN_35 = _pixelIndex_T ? io_pixelIn : firstByte; // @[Conditional.scala 40:58 CaptureModule.scala 138:25 CaptureModule.scala 56:27]
  wire [7:0] _GEN_36 = _pixelIndex_T ? secondByte : _GEN_33; // @[Conditional.scala 40:58 CaptureModule.scala 57:27]
  wire  _GEN_37 = _pixelIndex_T ? 1'h0 : _GEN_34; // @[Conditional.scala 40:58]
  wire  _GEN_41 = io_href & pclkRisingEdge & _GEN_37; // @[CaptureModule.scala 133:39]
  wire [9:0] _colCnt_T_1 = colCnt + 10'h1; // @[CaptureModule.scala 183:30]
  wire [16:0] _writePtr_T_1 = writePtr + 17'h1; // @[CaptureModule.scala 184:32]
  wire  _GEN_52 = FMS & _GEN_41; // @[Conditional.scala 39:67]
  wire  wrEnaWire = _T_6 ? 1'h0 : _GEN_52; // @[Conditional.scala 40:58]
  wire [9:0] _GEN_42 = wrEnaWire ? _colCnt_T_1 : colCnt; // @[CaptureModule.scala 182:22 CaptureModule.scala 183:20 CaptureModule.scala 68:36]
  wire [8:0] _rowCnt_T_1 = rowCnt + 9'h1; // @[CaptureModule.scala 187:26]
  reg  REG; // @[CaptureModule.scala 195:28]
  wire  _GEN_68 = newFrame & ~REG | _GEN_14; // @[CaptureModule.scala 195:40 CaptureModule.scala 196:24]
  reg [16:0] io_pixelAddr_REG; // @[CaptureModule.scala 208:31]
  SinglePortRam buffer ( // @[CaptureModule.scala 78:22]
    .clock(buffer_clock),
    .io_addr(buffer_io_addr),
    .io_dataIn(buffer_io_dataIn),
    .io_dataOut(buffer_io_dataOut),
    .io_wrEna(buffer_io_wrEna),
    .io_rdEna(buffer_io_rdEna)
  );
  assign io_pixelOut = buffer_io_dataOut; // @[CaptureModule.scala 209:21]
  assign io_pixelAddr = io_pixelAddr_REG; // @[CaptureModule.scala 208:21]
  assign io_frameWidth = colCnt; // @[CaptureModule.scala 206:21]
  assign io_frameHeight = rowCnt; // @[CaptureModule.scala 207:21]
  assign io_capturing = _T_6 ? 1'h0 : FMS; // @[Conditional.scala 40:58 CaptureModule.scala 126:17]
  assign io_pixelValid = pixelValid; // @[CaptureModule.scala 212:21]
  assign io_frameFull = frameFull; // @[CaptureModule.scala 211:21]
  assign io_newFrame = newFrame; // @[CaptureModule.scala 210:21]
  assign buffer_clock = clock;
  assign buffer_io_addr = (io_videoMode | io_readFrame) & frameFull ? _GEN_3 : bufferAddr_REG; // @[CaptureModule.scala 86:55 CaptureModule.scala 105:16]
  assign buffer_io_dataIn = {firstByte,secondByte}; // @[Cat.scala 30:58]
  assign buffer_io_wrEna = wrEna; // @[CaptureModule.scala 199:21]
  assign buffer_io_rdEna = io_readFrame; // @[CaptureModule.scala 200:21]
  always @(posedge clock) begin
    if (reset) begin // @[CaptureModule.scala 52:20]
      FMS <= 1'h0; // @[CaptureModule.scala 52:20]
    end else if (_T_6) begin // @[Conditional.scala 40:58]
      if (io_vsync) begin // @[CaptureModule.scala 112:22]
        FMS <= 1'h0; // @[CaptureModule.scala 113:13]
      end else if (captureSignalHolder | io_videoMode) begin // @[CaptureModule.scala 115:50]
        FMS <= _GEN_15;
      end
    end else if (FMS) begin // @[Conditional.scala 39:67]
      if (io_vsync) begin // @[CaptureModule.scala 131:23]
        FMS <= 1'h0;
      end else begin
        FMS <= 1'h1;
      end
    end
    if (reset) begin // @[CaptureModule.scala 54:27]
      writePtr <= 17'h0; // @[CaptureModule.scala 54:27]
    end else if (_T_6) begin // @[Conditional.scala 40:58]
      if (!(io_vsync)) begin // @[CaptureModule.scala 112:22]
        if (captureSignalHolder | io_videoMode) begin // @[CaptureModule.scala 115:50]
          writePtr <= _GEN_20;
        end
      end
    end else if (FMS) begin // @[Conditional.scala 39:67]
      if (wrEnaWire) begin // @[CaptureModule.scala 182:22]
        writePtr <= _writePtr_T_1; // @[CaptureModule.scala 184:20]
      end
    end
    if (reset) begin // @[CaptureModule.scala 55:27]
      readPtr <= 17'h0; // @[CaptureModule.scala 55:27]
    end else if ((io_videoMode | io_readFrame) & frameFull) begin // @[CaptureModule.scala 86:55]
      if (readPtr == bufferDepthCounter) begin // @[CaptureModule.scala 98:42]
        readPtr <= 17'h0; // @[CaptureModule.scala 99:26]
      end else if (capturing & _hrefFallingEdge_T) begin // @[CaptureModule.scala 87:34]
        readPtr <= _readPtr_T_1; // @[CaptureModule.scala 89:18]
      end else begin
        readPtr <= _GEN_1;
      end
    end
    if (reset) begin // @[CaptureModule.scala 56:27]
      firstByte <= 8'h0; // @[CaptureModule.scala 56:27]
    end else if (!(_T_6)) begin // @[Conditional.scala 40:58]
      if (FMS) begin // @[Conditional.scala 39:67]
        if (io_href & pclkRisingEdge) begin // @[CaptureModule.scala 133:39]
          firstByte <= _GEN_35;
        end
      end
    end
    if (reset) begin // @[CaptureModule.scala 57:27]
      secondByte <= 8'h0; // @[CaptureModule.scala 57:27]
    end else if (!(_T_6)) begin // @[Conditional.scala 40:58]
      if (FMS) begin // @[Conditional.scala 39:67]
        if (io_href & pclkRisingEdge) begin // @[CaptureModule.scala 133:39]
          secondByte <= _GEN_36;
        end
      end
    end
    if (reset) begin // @[CaptureModule.scala 62:36]
      captureSignalHolder <= 1'h0; // @[CaptureModule.scala 62:36]
    end else if (_T_6) begin // @[Conditional.scala 40:58]
      if (io_vsync) begin // @[CaptureModule.scala 112:22]
        captureSignalHolder <= _captureSignalHolder_T; // @[CaptureModule.scala 108:24]
      end else if (captureSignalHolder | io_videoMode) begin // @[CaptureModule.scala 115:50]
        captureSignalHolder <= _GEN_16;
      end else begin
        captureSignalHolder <= _captureSignalHolder_T; // @[CaptureModule.scala 108:24]
      end
    end else begin
      captureSignalHolder <= _captureSignalHolder_T; // @[CaptureModule.scala 108:24]
    end
    if (reset) begin // @[CaptureModule.scala 64:36]
      bufferDepthCounter <= 17'h0; // @[CaptureModule.scala 64:36]
    end else if (newFrame) begin // @[CaptureModule.scala 192:18]
      bufferDepthCounter <= writePtr; // @[CaptureModule.scala 193:24]
    end else if ((io_videoMode | io_readFrame) & frameFull) begin // @[CaptureModule.scala 86:55]
      if (readPtr == bufferDepthCounter) begin // @[CaptureModule.scala 98:42]
        bufferDepthCounter <= 17'h0; // @[CaptureModule.scala 100:26]
      end
    end
    if (reset) begin // @[CaptureModule.scala 65:36]
      newFrame <= 1'h0; // @[CaptureModule.scala 65:36]
    end else if (_T_6) begin // @[Conditional.scala 40:58]
      if (!(io_vsync)) begin // @[CaptureModule.scala 112:22]
        if (captureSignalHolder | io_videoMode) begin // @[CaptureModule.scala 115:50]
          newFrame <= _GEN_17;
        end
      end
    end else if (FMS) begin // @[Conditional.scala 39:67]
      newFrame <= io_vsync; // @[CaptureModule.scala 130:17]
    end
    if (reset) begin // @[CaptureModule.scala 66:36]
      pixelIndex <= 1'h0; // @[CaptureModule.scala 66:36]
    end else if (!(_T_6)) begin // @[Conditional.scala 40:58]
      if (FMS) begin // @[Conditional.scala 39:67]
        if (io_href & pclkRisingEdge) begin // @[CaptureModule.scala 133:39]
          pixelIndex <= ~pixelIndex; // @[CaptureModule.scala 135:22]
        end
      end
    end
    if (reset) begin // @[CaptureModule.scala 67:36]
      rowCnt <= 9'h0; // @[CaptureModule.scala 67:36]
    end else if (_T_6) begin // @[Conditional.scala 40:58]
      if (!(io_vsync)) begin // @[CaptureModule.scala 112:22]
        if (captureSignalHolder | io_videoMode) begin // @[CaptureModule.scala 115:50]
          rowCnt <= _GEN_18;
        end
      end
    end else if (FMS) begin // @[Conditional.scala 39:67]
      if (hrefRisingEdge) begin // @[CaptureModule.scala 186:27]
        rowCnt <= _rowCnt_T_1; // @[CaptureModule.scala 187:16]
      end
    end
    if (reset) begin // @[CaptureModule.scala 68:36]
      colCnt <= 10'h0; // @[CaptureModule.scala 68:36]
    end else if (_T_6) begin // @[Conditional.scala 40:58]
      if (!(io_vsync)) begin // @[CaptureModule.scala 112:22]
        if (captureSignalHolder | io_videoMode) begin // @[CaptureModule.scala 115:50]
          colCnt <= _GEN_19;
        end
      end
    end else if (FMS) begin // @[Conditional.scala 39:67]
      if (hrefRisingEdge) begin // @[CaptureModule.scala 186:27]
        colCnt <= 10'h0; // @[CaptureModule.scala 188:16]
      end else begin
        colCnt <= _GEN_42;
      end
    end
    if (reset) begin // @[CaptureModule.scala 69:36]
      frameFull <= 1'h0; // @[CaptureModule.scala 69:36]
    end else begin
      frameFull <= _GEN_68;
    end
    if (_T_6) begin // @[Conditional.scala 40:58]
      wrEna <= 1'h0;
    end else begin
      wrEna <= _GEN_52;
    end
    if (reset) begin // @[CaptureModule.scala 76:28]
      pixelValid <= 1'h0; // @[CaptureModule.scala 76:28]
    end else if ((io_videoMode | io_readFrame) & frameFull) begin // @[CaptureModule.scala 86:55]
      if (readPtr == bufferDepthCounter) begin // @[CaptureModule.scala 98:42]
        pixelValid <= 1'h0; // @[CaptureModule.scala 102:26]
      end else begin
        pixelValid <= _GEN_5;
      end
    end
    vsyncFallingEdge_REG <= io_vsync; // @[CaptureModule.scala 80:50]
    hrefRisingEdge_REG <= io_href; // @[CaptureModule.scala 82:50]
    pclkRisingEdge_REG <= io_pclk; // @[CaptureModule.scala 83:50]
    bufferAddr_REG <= writePtr; // @[CaptureModule.scala 105:26]
    REG <= newFrame; // @[CaptureModule.scala 195:28]
    io_pixelAddr_REG <= readPtr; // @[CaptureModule.scala 208:31]
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  FMS = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  writePtr = _RAND_1[16:0];
  _RAND_2 = {1{`RANDOM}};
  readPtr = _RAND_2[16:0];
  _RAND_3 = {1{`RANDOM}};
  firstByte = _RAND_3[7:0];
  _RAND_4 = {1{`RANDOM}};
  secondByte = _RAND_4[7:0];
  _RAND_5 = {1{`RANDOM}};
  captureSignalHolder = _RAND_5[0:0];
  _RAND_6 = {1{`RANDOM}};
  bufferDepthCounter = _RAND_6[16:0];
  _RAND_7 = {1{`RANDOM}};
  newFrame = _RAND_7[0:0];
  _RAND_8 = {1{`RANDOM}};
  pixelIndex = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  rowCnt = _RAND_9[8:0];
  _RAND_10 = {1{`RANDOM}};
  colCnt = _RAND_10[9:0];
  _RAND_11 = {1{`RANDOM}};
  frameFull = _RAND_11[0:0];
  _RAND_12 = {1{`RANDOM}};
  wrEna = _RAND_12[0:0];
  _RAND_13 = {1{`RANDOM}};
  pixelValid = _RAND_13[0:0];
  _RAND_14 = {1{`RANDOM}};
  vsyncFallingEdge_REG = _RAND_14[0:0];
  _RAND_15 = {1{`RANDOM}};
  hrefRisingEdge_REG = _RAND_15[0:0];
  _RAND_16 = {1{`RANDOM}};
  pclkRisingEdge_REG = _RAND_16[0:0];
  _RAND_17 = {1{`RANDOM}};
  bufferAddr_REG = _RAND_17[16:0];
  _RAND_18 = {1{`RANDOM}};
  REG = _RAND_18[0:0];
  _RAND_19 = {1{`RANDOM}};
  io_pixelAddr_REG = _RAND_19[16:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
