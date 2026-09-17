public final class ColorModels {
    private ColorModels() {}
    public static double clamp(double x) { return Math.max(0, Math.min(1, x)); }
    public static boolean outOfGamut(double[] rgb) {
        for (double x : rgb)
            if (x < -1e-6 || x > 1 + 1e-6)
                return true;
        return false;
    }
    public static double[] clip(double[] rgb) {

        return new double[]{clamp(rgb[0]), clamp(rgb[1]), clamp(rgb[2])};
    }
    public static double[] from(int model, double[] v) {
        if (model == 0) return new double[]{(1-v[0]/100)*(1-v[3]/100), (1-v[1]/100)*(1-v[3]/100), (1-v[2]/100)*(1-v[3]/100)};
        if (model == 1) return labToRgb(v);
        double h = ((v[0] % 360) + 360) % 360 / 60, s=v[1]/100, b=v[2]/100;
        double c=b*s, x=c*(1-Math.abs(h%2-1)), m=b-c;
        double[][] sectors={{c,x,0},{x,c,0},{0,c,x},{0,x,c},{x,0,c},{c,0,x}};
        double[] p=sectors[(int)h];
        return new double[]{p[0]+m,p[1]+m,p[2]+m};
    }
    public static double[] to(int model, double[] rgb) {
        double r=rgb[0], g=rgb[1], b=rgb[2], max=Math.max(r,Math.max(g,b)), min=Math.min(r,Math.min(g,b)), d=max-min;
        if(model==0) return max==0 ? new double[]{0,0,0,100} : new double[]{100*(max-r)/max,100*(max-g)/max,100*(max-b)/max,100*(1-max)};
        if(model==1) return rgbToLab(rgb);
        double h=d==0 ? 0 : max==r ? 60*((g-b)/d%6) : max==g ? 60*((b-r)/d+2) : 60*((r-g)/d+4);
        return new double[]{(h+360)%360,max==0?0:100*d/max,100*max};
    }
    private static double linear(double c) { return c<=0.04045?c/12.92:Math.pow((c+0.055)/1.055,2.4); }
    private static double encoded(double c) { return c<=0.0031308?12.92*c:1.055*Math.pow(c,1/2.4)-0.055; }
    private static double f(double t) { double d=6.0/29; return t>d*d*d?Math.cbrt(t):t/(3*d*d)+4.0/29; }
    private static double inv(double t) { double d=6.0/29; return t>d?t*t*t:3*d*d*(t-4.0/29); }
    public static double[] rgbToLab(double[] rgb) {
        double r=linear(rgb[0]),g=linear(rgb[1]),b=linear(rgb[2]);
        double x=f((.4124564*r+.3575761*g+.1804375*b)/.95047);
        double y=f(.2126729*r+.7151522*g+.0721750*b);
        double z=f((.0193339*r+.1191920*g+.9503041*b)/1.08883);
        return new double[]{116*y-16,500*(x-y),200*(y-z)};
    }
    public static double[] labToRgb(double[] lab) {
        double fy=(lab[0]+16)/116;
        double x=.95047*inv(fy+lab[1]/500), y=inv(fy), z=1.08883*inv(fy-lab[2]/200);
        return new double[]{encoded(3.2404542*x-1.5371385*y-.4985314*z),encoded(-.9692660*x+1.8760108*y+.0415560*z),encoded(.0556434*x-.2040259*y+1.0572252*z)};
    }
}