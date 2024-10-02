package cc.lib.rem2.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Created by Chris Caron on 11/14/23.
 */
class Remote2ProcessorProvider : SymbolProcessorProvider {

	override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
		return Remote2Processor(
			codeGenerator = environment.codeGenerator,
			logger = environment.logger,
			options = environment.options
		)
	}
}
