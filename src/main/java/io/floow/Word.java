package io.floow;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Data
@Document("word_dictionary")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Word implements Serializable {

    @Id
    private String id;
    private String word;
    private String filePath;
}
