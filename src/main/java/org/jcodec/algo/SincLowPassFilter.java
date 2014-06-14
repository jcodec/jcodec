package org.jcodec.algo;

/**
 * Low pass filter based on windowed sinc function
 * 
 * Simplest possible low pass filter
 * 
 * BW = 4 / kernelSize
 * 
 * BW - width of transition band measured in fraction of sampling rate
 * 
 * So for kernelSize = 20, and sampling rate 48000, BW = 9600 Hz ( pretty huge )
 * 
 * @author Jay Codec
 * 
 */
public class SincLowPassFilter extends ConvolutionFilter {

    private int kernelSize;
    private double cutoffFreq;

    public SincLowPassFilter(int cutoffFreq, int samplingRate) {
        this((double) cutoffFreq / samplingRate);
    }

    public SincLowPassFilter(double cutoffFreq) {
        this(40, cutoffFreq);
    }

    public SincLowPassFilter(int kernelSize, double cutoffFreq) {
        this.kernelSize = kernelSize;
        this.cutoffFreq = cutoffFreq;
    }

    @Override
    protected double[] buildKernel() {
        double[] kernel = new double[kernelSize];
        double sum = 0;
        for (int i = 0; i < kernelSize; i++) {
            int a = i - kernelSize / 2;
            if (a != 0)
                // kernel[i] = (Math.sin(2 * Math.PI * cutoffFreq * a) / a)
                // * (0.42 - 0.5 * Math.cos(2 * Math.PI / kernelSize) + 0.08 *
                // Math.cos(4 * Math.PI / kernelSize));

                kernel[i] = Math.sin(2 * Math.PI * cutoffFreq * (i - kernelSize / 2)) / (i - kernelSize / 2)
                        * (0.54 - 0.46 * Math.cos(2 * Math.PI * i / kernelSize));

            else
                kernel[i] = 2 * Math.PI * cutoffFreq;
            sum += kernel[i];
        }
        for (int i = 0; i < kernelSize; i++)
            kernel[i] /= sum;

        return kernel;
    }
}
