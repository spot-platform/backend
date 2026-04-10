package backend.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SampleTest {

	@Test
	@DisplayName("Sample 객체가 생성되어야 한다.")
	void createSample() {
		// given
		String name = "test name";

		// when
		Sample sample = Sample.builder()
			.name(name)
			.build();

		// then
		assertThat(sample.getName()).isEqualTo(name);
	}
}
