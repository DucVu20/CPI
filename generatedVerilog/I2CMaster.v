module I2CMaster(
  input        clock,
  input        reset,
  input        io_config,
  output       io_SCCBReady,
  output       io_SIOC,
  output       io_SIOD,
  input  [7:0] io_configData,
  input  [7:0] io_controlAddr,
  input        io_coreEna,
  input  [7:0] io_prescalerLow,
  input  [7:0] io_prescalerHigh
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
`endif // RANDOMIZE_REG_INIT
  reg  SIOC; // @[I2CMaster.scala 41:29]
  reg  SIOD; // @[I2CMaster.scala 42:29]
  reg [17:0] clkCnt; // @[I2CMaster.scala 43:29]
  reg  clkEna; // @[I2CMaster.scala 44:29]
  reg [7:0] latchedAddr; // @[I2CMaster.scala 45:29]
  reg [7:0] latchedData; // @[I2CMaster.scala 46:29]
  reg  SCCBReady; // @[I2CMaster.scala 47:29]
  reg  transmitBit; // @[I2CMaster.scala 49:29]
  reg  i2cWrite; // @[I2CMaster.scala 51:29]
  reg [3:0] bitCnt; // @[I2CMaster.scala 52:29]
  reg [1:0] byteCounter; // @[I2CMaster.scala 53:29]
  reg [7:0] transmitByte; // @[I2CMaster.scala 54:29]
  wire  _T_3 = ~io_coreEna | ~(|clkCnt); // @[I2CMaster.scala 66:21]
  wire [15:0] _clkCnt_T = {io_prescalerHigh,io_prescalerLow}; // @[Cat.scala 30:58]
  wire [17:0] _clkCnt_T_2 = clkCnt - 18'h1; // @[I2CMaster.scala 70:22]
  reg [4:0] FMS; // @[I2CMaster.scala 80:20]
  wire  _transmitBit_T_1 = ~(|bitCnt); // @[I2CMaster.scala 81:22]
  wire  _T_4 = 5'h0 == FMS; // @[Conditional.scala 37:30]
  wire  _GEN_4 = io_config ? 1'h0 : SCCBReady; // @[I2CMaster.scala 91:23 I2CMaster.scala 92:19 I2CMaster.scala 47:29]
  wire  _T_5 = 5'h1 == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_6 = i2cWrite ? 5'h10 : FMS; // @[I2CMaster.scala 98:23 I2CMaster.scala 99:15 I2CMaster.scala 80:20]
  wire  _T_6 = 5'h2 == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_8 = clkEna ? 5'h3 : FMS; // @[I2CMaster.scala 104:19 I2CMaster.scala 105:14 I2CMaster.scala 80:20]
  wire  _GEN_9 = clkEna | SIOC; // @[I2CMaster.scala 104:19 I2CMaster.scala 106:14 I2CMaster.scala 41:29]
  wire  _GEN_10 = clkEna | SIOD; // @[I2CMaster.scala 104:19 I2CMaster.scala 107:14 I2CMaster.scala 42:29]
  wire  _T_7 = 5'h3 == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_11 = clkEna ? 5'h4 : FMS; // @[I2CMaster.scala 111:19 I2CMaster.scala 112:14 I2CMaster.scala 80:20]
  wire  _T_8 = 5'h4 == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_12 = clkEna ? 5'h5 : FMS; // @[I2CMaster.scala 118:19 I2CMaster.scala 119:14 I2CMaster.scala 80:20]
  wire  _GEN_13 = clkEna ? 1'h0 : SIOD; // @[I2CMaster.scala 118:19 I2CMaster.scala 121:14 I2CMaster.scala 42:29]
  wire  _T_9 = 5'h5 == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_14 = clkEna ? 5'h6 : FMS; // @[I2CMaster.scala 125:19 I2CMaster.scala 126:14 I2CMaster.scala 80:20]
  wire  _T_10 = 5'h6 == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_15 = clkEna ? 5'hf : FMS; // @[I2CMaster.scala 132:19 I2CMaster.scala 133:14 I2CMaster.scala 80:20]
  wire  _GEN_16 = clkEna ? 1'h0 : SIOC; // @[I2CMaster.scala 132:19 I2CMaster.scala 134:14 I2CMaster.scala 41:29]
  wire  _GEN_17 = clkEna | i2cWrite; // @[I2CMaster.scala 132:19 I2CMaster.scala 136:18 I2CMaster.scala 51:29]
  wire  _T_11 = 5'h7 == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_18 = clkEna ? 5'h8 : FMS; // @[I2CMaster.scala 140:19 I2CMaster.scala 141:14 I2CMaster.scala 80:20]
  wire  _T_12 = 5'h8 == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_19 = clkEna ? 5'h9 : FMS; // @[I2CMaster.scala 147:19 I2CMaster.scala 148:14 I2CMaster.scala 80:20]
  wire  _T_13 = 5'h9 == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_20 = clkEna ? 5'ha : FMS; // @[I2CMaster.scala 154:19 I2CMaster.scala 155:14 I2CMaster.scala 80:20]
  wire  _T_14 = 5'ha == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_21 = clkEna ? 5'h0 : FMS; // @[I2CMaster.scala 161:19 I2CMaster.scala 162:19 I2CMaster.scala 80:20]
  wire  _GEN_22 = clkEna ? 1'h0 : i2cWrite; // @[I2CMaster.scala 161:19 I2CMaster.scala 165:19 I2CMaster.scala 51:29]
  wire  _GEN_23 = clkEna | SCCBReady; // @[I2CMaster.scala 161:19 I2CMaster.scala 166:19 I2CMaster.scala 47:29]
  wire  _T_15 = 5'hb == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_24 = clkEna ? 5'hc : FMS; // @[I2CMaster.scala 170:19 I2CMaster.scala 171:14 I2CMaster.scala 80:20]
  wire  _T_16 = 5'hc == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_25 = clkEna ? 5'hd : FMS; // @[I2CMaster.scala 177:19 I2CMaster.scala 178:14 I2CMaster.scala 80:20]
  wire  _T_17 = 5'hd == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_26 = clkEna ? 5'he : FMS; // @[I2CMaster.scala 184:19 I2CMaster.scala 185:14 I2CMaster.scala 80:20]
  wire  _T_18 = 5'he == FMS; // @[Conditional.scala 37:30]
  wire [4:0] _GEN_27 = clkEna ? 5'h1 : FMS; // @[I2CMaster.scala 191:19 I2CMaster.scala 192:14 I2CMaster.scala 80:20]
  wire  _T_19 = 5'hf == FMS; // @[Conditional.scala 37:30]
  wire [1:0] _byteCounter_T_1 = byteCounter - 2'h1; // @[I2CMaster.scala 199:34]
  wire [4:0] _GEN_28 = ~(|byteCounter) ? 5'h7 : 5'hb; // @[I2CMaster.scala 200:31 I2CMaster.scala 201:13 I2CMaster.scala 203:13]
  wire  _T_22 = 2'h3 == byteCounter; // @[Conditional.scala 37:30]
  wire  _T_23 = 2'h2 == byteCounter; // @[Conditional.scala 37:30]
  wire  _T_24 = 2'h1 == byteCounter; // @[Conditional.scala 37:30]
  wire  _T_25 = 2'h0 == byteCounter; // @[Conditional.scala 37:30]
  wire [7:0] _GEN_29 = _T_25 ? latchedData : transmitByte; // @[Conditional.scala 39:67 I2CMaster.scala 210:32 I2CMaster.scala 54:29]
  wire [7:0] _GEN_30 = _T_24 ? latchedData : _GEN_29; // @[Conditional.scala 39:67 I2CMaster.scala 209:32]
  wire [7:0] _GEN_31 = _T_23 ? latchedAddr : _GEN_30; // @[Conditional.scala 39:67 I2CMaster.scala 208:32]
  wire [7:0] _GEN_32 = _T_22 ? 8'h42 : _GEN_31; // @[Conditional.scala 40:58 I2CMaster.scala 206:32]
  wire  _T_26 = 5'h10 == FMS; // @[Conditional.scala 37:30]
  wire [3:0] _bitCnt_T_1 = bitCnt - 4'h1; // @[I2CMaster.scala 214:30]
  wire [8:0] _transmitByte_T = {transmitByte, 1'h0}; // @[I2CMaster.scala 215:36]
  wire [4:0] _GEN_33 = _transmitBit_T_1 ? 5'hf : 5'hb; // @[I2CMaster.scala 216:26 I2CMaster.scala 217:13 I2CMaster.scala 219:13]
  wire [3:0] _GEN_34 = _T_26 ? _bitCnt_T_1 : bitCnt; // @[Conditional.scala 39:67 I2CMaster.scala 214:20 I2CMaster.scala 52:29]
  wire [8:0] _GEN_35 = _T_26 ? _transmitByte_T : {{1'd0}, transmitByte}; // @[Conditional.scala 39:67 I2CMaster.scala 215:20 I2CMaster.scala 54:29]
  wire [4:0] _GEN_36 = _T_26 ? _GEN_33 : FMS; // @[Conditional.scala 39:67 I2CMaster.scala 80:20]
  wire [3:0] _GEN_37 = _T_19 ? 4'h8 : _GEN_34; // @[Conditional.scala 39:67 I2CMaster.scala 198:14]
  wire [1:0] _GEN_38 = _T_19 ? _byteCounter_T_1 : byteCounter; // @[Conditional.scala 39:67 I2CMaster.scala 199:19 I2CMaster.scala 53:29]
  wire [4:0] _GEN_39 = _T_19 ? _GEN_28 : _GEN_36; // @[Conditional.scala 39:67]
  wire [8:0] _GEN_40 = _T_19 ? {{1'd0}, _GEN_32} : _GEN_35; // @[Conditional.scala 39:67]
  wire [4:0] _GEN_41 = _T_18 ? _GEN_27 : _GEN_39; // @[Conditional.scala 39:67]
  wire  _GEN_42 = _T_18 ? _GEN_16 : SIOC; // @[Conditional.scala 39:67 I2CMaster.scala 41:29]
  wire  _GEN_43 = _T_18 ? transmitBit : SIOD; // @[Conditional.scala 39:67 I2CMaster.scala 195:12 I2CMaster.scala 42:29]
  wire [3:0] _GEN_44 = _T_18 ? bitCnt : _GEN_37; // @[Conditional.scala 39:67 I2CMaster.scala 52:29]
  wire [1:0] _GEN_45 = _T_18 ? byteCounter : _GEN_38; // @[Conditional.scala 39:67 I2CMaster.scala 53:29]
  wire [8:0] _GEN_46 = _T_18 ? {{1'd0}, transmitByte} : _GEN_40; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire [4:0] _GEN_47 = _T_17 ? _GEN_26 : _GEN_41; // @[Conditional.scala 39:67]
  wire  _GEN_48 = _T_17 ? _GEN_9 : _GEN_42; // @[Conditional.scala 39:67]
  wire  _GEN_49 = _T_17 ? transmitBit : _GEN_43; // @[Conditional.scala 39:67 I2CMaster.scala 188:12]
  wire [3:0] _GEN_50 = _T_17 ? bitCnt : _GEN_44; // @[Conditional.scala 39:67 I2CMaster.scala 52:29]
  wire [1:0] _GEN_51 = _T_17 ? byteCounter : _GEN_45; // @[Conditional.scala 39:67 I2CMaster.scala 53:29]
  wire [8:0] _GEN_52 = _T_17 ? {{1'd0}, transmitByte} : _GEN_46; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire [4:0] _GEN_53 = _T_16 ? _GEN_25 : _GEN_47; // @[Conditional.scala 39:67]
  wire  _GEN_54 = _T_16 ? _GEN_9 : _GEN_48; // @[Conditional.scala 39:67]
  wire  _GEN_55 = _T_16 ? transmitBit : _GEN_49; // @[Conditional.scala 39:67 I2CMaster.scala 181:12]
  wire [3:0] _GEN_56 = _T_16 ? bitCnt : _GEN_50; // @[Conditional.scala 39:67 I2CMaster.scala 52:29]
  wire [1:0] _GEN_57 = _T_16 ? byteCounter : _GEN_51; // @[Conditional.scala 39:67 I2CMaster.scala 53:29]
  wire [8:0] _GEN_58 = _T_16 ? {{1'd0}, transmitByte} : _GEN_52; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire [4:0] _GEN_59 = _T_15 ? _GEN_24 : _GEN_53; // @[Conditional.scala 39:67]
  wire  _GEN_60 = _T_15 ? _GEN_16 : _GEN_54; // @[Conditional.scala 39:67]
  wire  _GEN_61 = _T_15 ? transmitBit : _GEN_55; // @[Conditional.scala 39:67 I2CMaster.scala 174:12]
  wire [3:0] _GEN_62 = _T_15 ? bitCnt : _GEN_56; // @[Conditional.scala 39:67 I2CMaster.scala 52:29]
  wire [1:0] _GEN_63 = _T_15 ? byteCounter : _GEN_57; // @[Conditional.scala 39:67 I2CMaster.scala 53:29]
  wire [8:0] _GEN_64 = _T_15 ? {{1'd0}, transmitByte} : _GEN_58; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire [4:0] _GEN_65 = _T_14 ? _GEN_21 : _GEN_59; // @[Conditional.scala 39:67]
  wire  _GEN_66 = _T_14 ? _GEN_9 : _GEN_60; // @[Conditional.scala 39:67]
  wire  _GEN_67 = _T_14 ? _GEN_10 : _GEN_61; // @[Conditional.scala 39:67]
  wire  _GEN_68 = _T_14 ? _GEN_22 : i2cWrite; // @[Conditional.scala 39:67 I2CMaster.scala 51:29]
  wire  _GEN_69 = _T_14 ? _GEN_23 : SCCBReady; // @[Conditional.scala 39:67 I2CMaster.scala 47:29]
  wire [3:0] _GEN_70 = _T_14 ? bitCnt : _GEN_62; // @[Conditional.scala 39:67 I2CMaster.scala 52:29]
  wire [1:0] _GEN_71 = _T_14 ? byteCounter : _GEN_63; // @[Conditional.scala 39:67 I2CMaster.scala 53:29]
  wire [8:0] _GEN_72 = _T_14 ? {{1'd0}, transmitByte} : _GEN_64; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire [4:0] _GEN_73 = _T_13 ? _GEN_20 : _GEN_65; // @[Conditional.scala 39:67]
  wire  _GEN_74 = _T_13 ? _GEN_9 : _GEN_66; // @[Conditional.scala 39:67]
  wire  _GEN_75 = _T_13 ? _GEN_13 : _GEN_67; // @[Conditional.scala 39:67]
  wire  _GEN_76 = _T_13 ? i2cWrite : _GEN_68; // @[Conditional.scala 39:67 I2CMaster.scala 51:29]
  wire  _GEN_77 = _T_13 ? SCCBReady : _GEN_69; // @[Conditional.scala 39:67 I2CMaster.scala 47:29]
  wire [3:0] _GEN_78 = _T_13 ? bitCnt : _GEN_70; // @[Conditional.scala 39:67 I2CMaster.scala 52:29]
  wire [1:0] _GEN_79 = _T_13 ? byteCounter : _GEN_71; // @[Conditional.scala 39:67 I2CMaster.scala 53:29]
  wire [8:0] _GEN_80 = _T_13 ? {{1'd0}, transmitByte} : _GEN_72; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire [4:0] _GEN_81 = _T_12 ? _GEN_19 : _GEN_73; // @[Conditional.scala 39:67]
  wire  _GEN_82 = _T_12 ? _GEN_9 : _GEN_74; // @[Conditional.scala 39:67]
  wire  _GEN_83 = _T_12 ? _GEN_13 : _GEN_75; // @[Conditional.scala 39:67]
  wire  _GEN_84 = _T_12 ? i2cWrite : _GEN_76; // @[Conditional.scala 39:67 I2CMaster.scala 51:29]
  wire  _GEN_85 = _T_12 ? SCCBReady : _GEN_77; // @[Conditional.scala 39:67 I2CMaster.scala 47:29]
  wire [3:0] _GEN_86 = _T_12 ? bitCnt : _GEN_78; // @[Conditional.scala 39:67 I2CMaster.scala 52:29]
  wire [1:0] _GEN_87 = _T_12 ? byteCounter : _GEN_79; // @[Conditional.scala 39:67 I2CMaster.scala 53:29]
  wire [8:0] _GEN_88 = _T_12 ? {{1'd0}, transmitByte} : _GEN_80; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire [4:0] _GEN_89 = _T_11 ? _GEN_18 : _GEN_81; // @[Conditional.scala 39:67]
  wire  _GEN_90 = _T_11 ? _GEN_16 : _GEN_82; // @[Conditional.scala 39:67]
  wire  _GEN_91 = _T_11 ? _GEN_13 : _GEN_83; // @[Conditional.scala 39:67]
  wire  _GEN_92 = _T_11 ? i2cWrite : _GEN_84; // @[Conditional.scala 39:67 I2CMaster.scala 51:29]
  wire  _GEN_93 = _T_11 ? SCCBReady : _GEN_85; // @[Conditional.scala 39:67 I2CMaster.scala 47:29]
  wire [3:0] _GEN_94 = _T_11 ? bitCnt : _GEN_86; // @[Conditional.scala 39:67 I2CMaster.scala 52:29]
  wire [1:0] _GEN_95 = _T_11 ? byteCounter : _GEN_87; // @[Conditional.scala 39:67 I2CMaster.scala 53:29]
  wire [8:0] _GEN_96 = _T_11 ? {{1'd0}, transmitByte} : _GEN_88; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire [4:0] _GEN_97 = _T_10 ? _GEN_15 : _GEN_89; // @[Conditional.scala 39:67]
  wire  _GEN_98 = _T_10 ? _GEN_16 : _GEN_90; // @[Conditional.scala 39:67]
  wire  _GEN_99 = _T_10 ? _GEN_13 : _GEN_91; // @[Conditional.scala 39:67]
  wire  _GEN_100 = _T_10 ? _GEN_17 : _GEN_92; // @[Conditional.scala 39:67]
  wire  _GEN_101 = _T_10 ? SCCBReady : _GEN_93; // @[Conditional.scala 39:67 I2CMaster.scala 47:29]
  wire [3:0] _GEN_102 = _T_10 ? bitCnt : _GEN_94; // @[Conditional.scala 39:67 I2CMaster.scala 52:29]
  wire [1:0] _GEN_103 = _T_10 ? byteCounter : _GEN_95; // @[Conditional.scala 39:67 I2CMaster.scala 53:29]
  wire [8:0] _GEN_104 = _T_10 ? {{1'd0}, transmitByte} : _GEN_96; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire [4:0] _GEN_105 = _T_9 ? _GEN_14 : _GEN_97; // @[Conditional.scala 39:67]
  wire  _GEN_106 = _T_9 ? _GEN_9 : _GEN_98; // @[Conditional.scala 39:67]
  wire  _GEN_107 = _T_9 ? _GEN_13 : _GEN_99; // @[Conditional.scala 39:67]
  wire  _GEN_108 = _T_9 ? i2cWrite : _GEN_100; // @[Conditional.scala 39:67 I2CMaster.scala 51:29]
  wire  _GEN_109 = _T_9 ? SCCBReady : _GEN_101; // @[Conditional.scala 39:67 I2CMaster.scala 47:29]
  wire [3:0] _GEN_110 = _T_9 ? bitCnt : _GEN_102; // @[Conditional.scala 39:67 I2CMaster.scala 52:29]
  wire [1:0] _GEN_111 = _T_9 ? byteCounter : _GEN_103; // @[Conditional.scala 39:67 I2CMaster.scala 53:29]
  wire [8:0] _GEN_112 = _T_9 ? {{1'd0}, transmitByte} : _GEN_104; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire [4:0] _GEN_113 = _T_8 ? _GEN_12 : _GEN_105; // @[Conditional.scala 39:67]
  wire  _GEN_114 = _T_8 ? _GEN_9 : _GEN_106; // @[Conditional.scala 39:67]
  wire  _GEN_115 = _T_8 ? _GEN_13 : _GEN_107; // @[Conditional.scala 39:67]
  wire  _GEN_116 = _T_8 ? i2cWrite : _GEN_108; // @[Conditional.scala 39:67 I2CMaster.scala 51:29]
  wire  _GEN_117 = _T_8 ? SCCBReady : _GEN_109; // @[Conditional.scala 39:67 I2CMaster.scala 47:29]
  wire [3:0] _GEN_118 = _T_8 ? bitCnt : _GEN_110; // @[Conditional.scala 39:67 I2CMaster.scala 52:29]
  wire [1:0] _GEN_119 = _T_8 ? byteCounter : _GEN_111; // @[Conditional.scala 39:67 I2CMaster.scala 53:29]
  wire [8:0] _GEN_120 = _T_8 ? {{1'd0}, transmitByte} : _GEN_112; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire [4:0] _GEN_121 = _T_7 ? _GEN_11 : _GEN_113; // @[Conditional.scala 39:67]
  wire  _GEN_122 = _T_7 ? _GEN_9 : _GEN_114; // @[Conditional.scala 39:67]
  wire  _GEN_123 = _T_7 ? _GEN_10 : _GEN_115; // @[Conditional.scala 39:67]
  wire  _GEN_124 = _T_7 ? i2cWrite : _GEN_116; // @[Conditional.scala 39:67 I2CMaster.scala 51:29]
  wire  _GEN_125 = _T_7 ? SCCBReady : _GEN_117; // @[Conditional.scala 39:67 I2CMaster.scala 47:29]
  wire [3:0] _GEN_126 = _T_7 ? bitCnt : _GEN_118; // @[Conditional.scala 39:67 I2CMaster.scala 52:29]
  wire [1:0] _GEN_127 = _T_7 ? byteCounter : _GEN_119; // @[Conditional.scala 39:67 I2CMaster.scala 53:29]
  wire [8:0] _GEN_128 = _T_7 ? {{1'd0}, transmitByte} : _GEN_120; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire  _GEN_130 = _T_6 ? _GEN_9 : _GEN_122; // @[Conditional.scala 39:67]
  wire  _GEN_131 = _T_6 ? _GEN_10 : _GEN_123; // @[Conditional.scala 39:67]
  wire  _GEN_133 = _T_6 ? SCCBReady : _GEN_125; // @[Conditional.scala 39:67 I2CMaster.scala 47:29]
  wire [8:0] _GEN_136 = _T_6 ? {{1'd0}, transmitByte} : _GEN_128; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire  _GEN_138 = _T_5 ? SIOC : _GEN_130; // @[Conditional.scala 39:67 I2CMaster.scala 41:29]
  wire  _GEN_139 = _T_5 ? SIOD : _GEN_131; // @[Conditional.scala 39:67 I2CMaster.scala 42:29]
  wire  _GEN_141 = _T_5 ? SCCBReady : _GEN_133; // @[Conditional.scala 39:67 I2CMaster.scala 47:29]
  wire [8:0] _GEN_144 = _T_5 ? {{1'd0}, transmitByte} : _GEN_136; // @[Conditional.scala 39:67 I2CMaster.scala 54:29]
  wire  _GEN_145 = _T_4 | _GEN_138; // @[Conditional.scala 40:58 I2CMaster.scala 85:19]
  wire  _GEN_146 = _T_4 | _GEN_139; // @[Conditional.scala 40:58 I2CMaster.scala 86:19]
  wire  _GEN_151 = _T_4 ? _GEN_4 : _GEN_141; // @[Conditional.scala 40:58]
  wire [8:0] _GEN_154 = _T_4 ? {{1'd0}, transmitByte} : _GEN_144; // @[Conditional.scala 40:58 I2CMaster.scala 54:29]
  assign io_SCCBReady = SCCBReady; // @[I2CMaster.scala 63:16]
  assign io_SIOC = io_coreEna & SIOC; // @[I2CMaster.scala 56:19 I2CMaster.scala 57:13 I2CMaster.scala 60:13]
  assign io_SIOD = io_coreEna & SIOD; // @[I2CMaster.scala 56:19 I2CMaster.scala 58:13 I2CMaster.scala 61:13]
  always @(posedge clock) begin
    SIOC <= reset | _GEN_145; // @[I2CMaster.scala 41:29 I2CMaster.scala 41:29]
    SIOD <= reset | _GEN_146; // @[I2CMaster.scala 42:29 I2CMaster.scala 42:29]
    if (reset) begin // @[I2CMaster.scala 43:29]
      clkCnt <= 18'h0; // @[I2CMaster.scala 43:29]
    end else if (~io_coreEna | ~(|clkCnt)) begin // @[I2CMaster.scala 66:38]
      clkCnt <= {{2'd0}, _clkCnt_T}; // @[I2CMaster.scala 67:12]
    end else begin
      clkCnt <= _clkCnt_T_2; // @[I2CMaster.scala 70:12]
    end
    clkEna <= reset | _T_3; // @[I2CMaster.scala 44:29 I2CMaster.scala 44:29]
    if (reset) begin // @[I2CMaster.scala 45:29]
      latchedAddr <= 8'h0; // @[I2CMaster.scala 45:29]
    end else if (_T_4) begin // @[Conditional.scala 40:58]
      latchedAddr <= io_controlAddr; // @[I2CMaster.scala 87:19]
    end
    if (reset) begin // @[I2CMaster.scala 46:29]
      latchedData <= 8'h0; // @[I2CMaster.scala 46:29]
    end else if (_T_4) begin // @[Conditional.scala 40:58]
      latchedData <= io_configData; // @[I2CMaster.scala 88:19]
    end
    SCCBReady <= reset | _GEN_151; // @[I2CMaster.scala 47:29 I2CMaster.scala 47:29]
    if (reset) begin // @[I2CMaster.scala 49:29]
      transmitBit <= 1'h0; // @[I2CMaster.scala 49:29]
    end else if (~(|bitCnt)) begin // @[I2CMaster.scala 81:21]
      transmitBit <= 1'h0;
    end else begin
      transmitBit <= transmitByte[7];
    end
    if (reset) begin // @[I2CMaster.scala 51:29]
      i2cWrite <= 1'h0; // @[I2CMaster.scala 51:29]
    end else if (!(_T_4)) begin // @[Conditional.scala 40:58]
      if (!(_T_5)) begin // @[Conditional.scala 39:67]
        if (!(_T_6)) begin // @[Conditional.scala 39:67]
          i2cWrite <= _GEN_124;
        end
      end
    end
    if (reset) begin // @[I2CMaster.scala 52:29]
      bitCnt <= 4'h0; // @[I2CMaster.scala 52:29]
    end else if (_T_4) begin // @[Conditional.scala 40:58]
      bitCnt <= 4'h8; // @[I2CMaster.scala 90:19]
    end else if (!(_T_5)) begin // @[Conditional.scala 39:67]
      if (!(_T_6)) begin // @[Conditional.scala 39:67]
        bitCnt <= _GEN_126;
      end
    end
    if (reset) begin // @[I2CMaster.scala 53:29]
      byteCounter <= 2'h0; // @[I2CMaster.scala 53:29]
    end else if (_T_4) begin // @[Conditional.scala 40:58]
      byteCounter <= 2'h3; // @[I2CMaster.scala 89:19]
    end else if (!(_T_5)) begin // @[Conditional.scala 39:67]
      if (!(_T_6)) begin // @[Conditional.scala 39:67]
        byteCounter <= _GEN_127;
      end
    end
    if (reset) begin // @[I2CMaster.scala 54:29]
      transmitByte <= 8'h0; // @[I2CMaster.scala 54:29]
    end else begin
      transmitByte <= _GEN_154[7:0];
    end
    if (reset) begin // @[I2CMaster.scala 80:20]
      FMS <= 5'h0; // @[I2CMaster.scala 80:20]
    end else if (_T_4) begin // @[Conditional.scala 40:58]
      if (io_config) begin // @[I2CMaster.scala 91:23]
        FMS <= 5'h2; // @[I2CMaster.scala 93:13]
      end
    end else if (_T_5) begin // @[Conditional.scala 39:67]
      if (clkEna) begin // @[I2CMaster.scala 97:19]
        FMS <= _GEN_6;
      end
    end else if (_T_6) begin // @[Conditional.scala 39:67]
      FMS <= _GEN_8;
    end else begin
      FMS <= _GEN_121;
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
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  SIOC = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  SIOD = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  clkCnt = _RAND_2[17:0];
  _RAND_3 = {1{`RANDOM}};
  clkEna = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  latchedAddr = _RAND_4[7:0];
  _RAND_5 = {1{`RANDOM}};
  latchedData = _RAND_5[7:0];
  _RAND_6 = {1{`RANDOM}};
  SCCBReady = _RAND_6[0:0];
  _RAND_7 = {1{`RANDOM}};
  transmitBit = _RAND_7[0:0];
  _RAND_8 = {1{`RANDOM}};
  i2cWrite = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  bitCnt = _RAND_9[3:0];
  _RAND_10 = {1{`RANDOM}};
  byteCounter = _RAND_10[1:0];
  _RAND_11 = {1{`RANDOM}};
  transmitByte = _RAND_11[7:0];
  _RAND_12 = {1{`RANDOM}};
  FMS = _RAND_12[4:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
