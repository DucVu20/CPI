module camera_cpu_top(
    input        clock,
    input        reset,
    input        io_rx,
    output       io_tx,
    input        io_p_clk,
    input        io_href,
    input        io_vsync,
    input  [7:0] io_data_in,
    output       io_SIOC,
    output       io_SIOD,
    output       camera_clock,
    output       locked
);

    wire system_clk;
    // instanciate a clock_wizard
    clk_wiz_0 clock_wizard(.clk_in1(clock),
                           .system_clk(system_clk),
                           .camera_clk(camera_clock),
                           .reset(reset),
                           .locked(locked)
    );

    // call the camera_top module and assign the output of the clk wizard to this module

    camera_top camera(
        .clock(system_clk),
        .reset(reset),
        .io_rx(io_rx),
        .io_tx(io_tx),
        .io_p_clk(io_p_clk),
        .io_href(io_href),
        .io_vsync(io_vsync),
        .io_data_in(io_data_in),
        .io_SIOC(io_SIOC),
        .io_SIOD(io_SIOD),
    );

endmodule