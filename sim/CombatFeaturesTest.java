import dev.meshac.CombatFeatures;
public final class CombatFeaturesTest {
	static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
	static CombatFeatures.Summary observe(CombatFeatures f,long t,int id,String weapon,double lat,double velocity,double cd){return f.observe(t,id,weapon,lat,0,velocity,cd,625);}
	public static void main(String[] args){
		CombatFeatures f=new CombatFeatures(); CombatFeatures.Summary s=null;
		for(int i=1;i<=40;i++)s=observe(f,i*625_000_000L,1,"sword",.7,0,1);
		check(s.n()==40 && s.lateralVariance()<1e-12,"stationary grinder fixed aim legal observation");
		check(s.fullChargeShare()==1 && Math.abs(s.meanNominalSlackMs())<1e-9,"disciplined cooldown legitimate");
		check(!s.trackingDefined() && !s.lag1Defined(),"stationary/zero variance is undefined, not suspicious");
		f=new CombatFeatures();for(int i=1;i<=40;i++)s=observe(f,i*120_000_000L,1,"axe",Math.sin(i),Math.sin(i),.25);
		check(s.fullChargeShare()==0,"legal spam clicking");
		check(s.trackingDefined() && s.trackingCorrelation()>.999,"tracking covariance direction");
		f=new CombatFeatures();for(int i=1;i<=20;i++)s=observe(f,3_000_000_000L+i*1_000_000L,1,"sword",0,0,1);
		check(s.meanGapMs()==1,"bunched server delivery retained without judgment");
		s=observe(f,4_000_000_000L,2,"sword",0,0,1);check(s.n()==1,"target isolation");
		s=observe(f,4_100_000_000L,2,"axe",0,0,1);check(s.n()==1,"weapon isolation");
		s=observe(f,8_000_000_000L,2,"axe",0,0,1);check(s.n()==1,"fight timeout");
		check(f.observe(9_000_000_000L,2,"axe",Double.NaN,0,0,1,625)==null,"reject nonfinite");
		f=new CombatFeatures();for(int i=1;i<=1000;i++)s=observe(f,i*100_000_000L,1,"sword",0,0,1);
		check(s.n()==64,"bounded memory");
		System.out.println("CombatFeaturesTest:11 legal-control and math assertions PASS; descriptive only");
	}
}
