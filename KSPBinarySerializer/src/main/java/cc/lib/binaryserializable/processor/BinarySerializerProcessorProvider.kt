package cc.lib.binaryserializable.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Created by Chris Caron on 11/14/23.
 */
class BinarySerializerProcessorProvider : SymbolProcessorProvider {

	override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
		return BinarySerializerProcessor(
			codeGenerator = environment.codeGenerator,
			logger = environment.logger,
			options = environment.options
		)
	}
}
