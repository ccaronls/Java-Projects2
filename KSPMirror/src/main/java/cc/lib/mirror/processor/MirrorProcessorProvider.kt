package cc.lib.mirror.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Created by Chris Caron on 11/14/23.
 */
class MirrorProcessorProvider : SymbolProcessorProvider {

	override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
		return MirrorProcessor(
			codeGenerator = environment.codeGenerator,
			logger = environment.logger,
			options = environment.options
		)
	}
}
