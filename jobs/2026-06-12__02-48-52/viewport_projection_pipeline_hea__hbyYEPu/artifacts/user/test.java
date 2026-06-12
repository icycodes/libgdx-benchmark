import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
public class test {
    public static void main(String[] args) {
        Viewport vp = new com.badlogic.gdx.utils.viewport.FitViewport(800, 600, new OrthographicCamera());
        vp.update(1024, 768, true);
        Vector2 v = vp.unproject(new Vector2(100, 200));
        System.out.println(v);
    }
}
