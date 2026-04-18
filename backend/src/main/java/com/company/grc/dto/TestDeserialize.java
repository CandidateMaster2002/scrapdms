import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.grc.dto.DeepvueGstDto;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestDeserialize {
    public static void main(String[] args) throws Exception {
        String content = Files.readString(Paths.get("deepvue_gst_output.json"));
        ObjectMapper mapper = new ObjectMapper();
        
        System.out.println("Parsing JSON...");
        DeepvueGstDto.ApiResponse apiResponse = mapper.readValue(content, DeepvueGstDto.ApiResponse.class);
        System.out.println("Parsed successfully. SubCode: " + apiResponse.getSubCode());
        System.out.println("BusinessName: " + apiResponse.getData().getBusinessName());
    }
}
