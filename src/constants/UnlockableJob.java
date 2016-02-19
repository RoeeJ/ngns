package constants;

import java.util.Arrays;
import java.util.List;

/**
 * Created by NROL on 3/20/2015.
 */
public class UnlockableJob {
    private static final List<UnlockableJob> jobs = Arrays.asList(new UnlockableJob(112, 100000000, "Hero"), new UnlockableJob(122, 100000000, "Paladin"), new UnlockableJob(132, 100000000, "Dark Knight"), new UnlockableJob(212, 100000000, "Arch Mage(Fire,Poision)"), new UnlockableJob(222, 100000000, "Arch Mage(Ice,Lightning)"), new UnlockableJob(232, 100000000, "Bishop"), new UnlockableJob(312, 100000000, "Bowmaster"), new UnlockableJob(322, 100000000, "Marksman"), new UnlockableJob(412, 100000000, "Night Lord"), new UnlockableJob(422, 100000000, "Shadower"), new UnlockableJob(512, 100000000, "Buccaneer"), new UnlockableJob(522, 100000000, "Corsair"), new UnlockableJob(1111, 75000000, "Dawn Warrior"), new UnlockableJob(1211, 75000000, "Blaze Wizard"), new UnlockableJob(1311, 75000000, "Wind Archer"), new UnlockableJob(1411, 75000000, "Night Walker"), new UnlockableJob(1511, 75000000, "Thunder Breaker"), new UnlockableJob(2112, 100000000, "Aran"));
    private final int id;
    private final int price;
    private final String name;

    public UnlockableJob(int id, int price, String name) {
        this.id = id;
        this.price = price;
        this.name = name;
    }

    public static UnlockableJob getJobById(int id){
        for(UnlockableJob job: jobs){
            if(job.id == id){return job;}
        }
        return null;
    }
    public static List<UnlockableJob> getJobs(){
        return jobs;
    }
    public int getId(){return id;}
    public int getPrice(){return price;}
    public String getName(){return name;}
}
